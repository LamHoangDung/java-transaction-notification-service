package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.example.config.AppConfig;
import org.example.entity.TransactionEntity;
import org.example.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

public class RabbitMQConsumerService {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQConsumerService.class);

    private final AppConfig config;
    private final EmailService emailService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TransactionRepository transactionRepository;

    public RabbitMQConsumerService(
            AppConfig config,
            EmailService emailService,
            TransactionRepository transactionRepository
    ) {
        this.config = config;
        this.emailService = emailService;
        this.transactionRepository = transactionRepository;
    }

    public void start(){

        // 1. Cấu hình kết nối
        ConnectionFactory connectionFactory = new ConnectionFactory();

        connectionFactory.setHost(config.get("rabbitmq.host"));
        connectionFactory.setPort(config.getInt("rabbitmq.port"));
        connectionFactory.setUsername(config.get("rabbitmq.username"));
        connectionFactory.setPassword(config.get("rabbitmq.password"));

        String reportEmailQueue = "reconciliation.report.email.queue";
        String paymentQueue = "payment.notification.queue";

        // Giữ connection và channel mở trong lúc lắng nghe
        try (Connection connection = connectionFactory.newConnection();
             Channel reconChannel = connection.createChannel();
             Channel paymentChannel = connection.createChannel();
             Scanner scanner = new Scanner(System.in)) {

            //Khai báo queue giống cấu hình bên producer
            reconChannel.queueDeclare(
                    reportEmailQueue, true, false, false, null
            );
            reconChannel.basicQos(1); //mỗi consumer chỉ nhận tối đa một message chưa ACK

            paymentChannel.queueDeclare(
                    paymentQueue, true, false, false, null
            );
            paymentChannel.basicQos(1); //mỗi consumer chỉ nhận tối đa một message chưa ACK

            //Công việc thực hiện khi nhận message
            DeliverCallback reportEmailCallback = (consumerTag, delivery) -> {
                long deliveryTag = delivery.getEnvelope().getDeliveryTag();
                try {
                    JsonNode report = objectMapper.readTree(delivery.getBody());
                    // Hàm này đã gửi mail và lưu lịch sử vào DB
                    emailService.sendReport(report);
                    // Không có lỗi -> xác nhận xử lý xong
                    reconChannel.basicAck(deliveryTag, false);
                } catch (Exception e) {
                    log.error("Lỗi gửi mail hoặc lưu lịch sử", e);
                }
            };

            DeliverCallback paymentCallback = (consumerTag, delivery) -> {
                long deliveryTag = delivery.getEnvelope().getDeliveryTag();
                try {
                    JsonNode payment = objectMapper.readTree(delivery.getBody());
                    String transactionId = payment.get("transactionId").asText();

                    //tim GD + check trang thai của GD truoc khi gui mail
                    TransactionEntity transaction = transactionRepository.findByTransactionId(transactionId);
                    if (transaction == null) {
                        System.out.println("khong tim thay giao dich");
                        return;
                    }

                    if ("SUCCESS".equals(transaction.getStatus()) && "PENDING".equals(transaction.getNotiStatus())) {
                        emailService.sendPayment(payment);
                        int updatedRows = transactionRepository.markNotificationSuccess(transactionId);
                        if (updatedRows != 1) {
                            System.out.println("cap nhat DB that bai");
                        }
                        paymentChannel.basicAck(deliveryTag, false);
                    }else if("SUCCESS".equals(transaction.getNotiStatus())) {
                        System.out.println("Giao dịch đã gửi mail, bỏ qua");
                    }else{
                        System.out.println("trang thai ko phu hop de gui mail");
                    }

                } catch (Exception e) {
                    log.error("Lỗi xử lý thanh toán. Tạm dừng nhận tin hãy kiểm tra lỗi rồi khởi động lại. ", e);
                }
            };

            //khai bao consumer
            reconChannel.basicConsume(
                    reportEmailQueue,
                    false, //không tự động ACK
                    reportEmailCallback,
                    consumerTag -> {}
            );

            paymentChannel.basicConsume(
                    paymentQueue,
                    false, // Không tự động ACK.
                    paymentCallback,
                    consumerTag -> {}
            );

            System.out.println("Đang lắng nghe. Nhấn Enter để dừng.");
            scanner.nextLine();

        } catch (Exception e) {
            log.error("Không thể chạy consumer RabbitMQ", e);
        }
    }
}