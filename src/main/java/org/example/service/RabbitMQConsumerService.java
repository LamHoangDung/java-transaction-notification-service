package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.example.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

public class RabbitMQConsumerService {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQConsumerService.class);

    private final AppConfig config;
    private final EmailService emailService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RabbitMQConsumerService(
            AppConfig config,
            EmailService emailService
    ) {
        this.config = config;
        this.emailService = emailService;
    }

    public void start(){

        // 1. Cấu hình kết nối
        ConnectionFactory connectionFactory = new ConnectionFactory();

        connectionFactory.setHost(config.get("rabbitmq.host"));
        connectionFactory.setPort(config.getInt("rabbitmq.port"));
        connectionFactory.setUsername(config.get("rabbitmq.username"));
        connectionFactory.setPassword(config.get("rabbitmq.password"));

        String reportEmailQueue = "reconciliation.report.email.queue";

        // Giữ connection và channel mở trong lúc lắng nghe
        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel();
             Scanner scanner = new Scanner(System.in)) {

            //Khai báo queue giống cấu hình bên producer
            channel.queueDeclare(
                    reportEmailQueue,
                    true,
                    false,
                    false,
                    null
            );

            //Công việc thực hiện khi nhận message
            DeliverCallback reportEmailCallback = (consumerTag, delivery) -> {
                try {
                    JsonNode report =
                            objectMapper.readTree(delivery.getBody());

                    // Hàm này đã gửi mail và lưu lịch sử vào DB
                    emailService.sendReport(report);

                } catch (Exception e) {
                    log.error("Lỗi gửi mail hoặc lưu lịch sử", e);
                }
            };

            //khai bao consumer
            channel.basicConsume(
                    reportEmailQueue,
                    true, // Tự động ACK
                    reportEmailCallback,
                    consumerTag -> {}
            );

            System.out.println("Đang lắng nghe. Nhấn Enter để dừng.");
            scanner.nextLine();

        } catch (Exception e) {
            log.error("Không thể chạy consumer RabbitMQ", e);
        }
    }
}