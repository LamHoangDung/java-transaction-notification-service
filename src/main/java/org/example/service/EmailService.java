package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.example.config.AppConfig;
import org.example.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Properties;

public class EmailService {

    private static final Logger log =
            LoggerFactory.getLogger(EmailService.class);

    private final AppConfig config;
    private final ReportRepository reportRepository;

    public EmailService(AppConfig config, ReportRepository reportRepository) {
        this.config = config;
        this.reportRepository = reportRepository;
    }

    public void sendPayment(JsonNode payment) throws MessagingException {

        // 1. Lấy thông tin từ message.
        String transactionId = payment.get("transactionId").asText();
        String transactionName = payment.get("transactionName").asText();
        String transactionTime = payment.get("transactionTime").asText();
        String toEmail = payment.get("toEmail").asText();

        // 2. Cấu hình SMTP giả lập.
        Properties properties = new Properties();
        properties.put("mail.smtp.host", config.get("mail.smtp.host"));
        properties.put("mail.smtp.port", config.get("mail.smtp.port"));
        properties.put("mail.smtp.auth", "false");
        properties.put("mail.smtp.starttls.enable", "false");

        Session session = Session.getInstance(properties);

        // 3. Soạn mail.
        MimeMessage message = new MimeMessage(session);

        message.setFrom(new InternetAddress(config.get("mail.from")));
        message.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(toEmail)
        );

        message.setSubject("Thông báo thanh toán thành công", "UTF-8");

        String content = """
            Thanh toán thành công

            Mã giao dịch: %s
            Tên giao dịch: %s
            Thời điểm thanh toán: %s
            """.formatted(
                transactionId,
                transactionName,
                transactionTime
        );

        message.setText(content, "UTF-8");

        // 4. Gửi mail.
        Transport.send(message);

        log.info("Đã gửi mail thanh toán cho giao dịch: {}", transactionId);
    }

    public void sendReport(JsonNode report) throws MessagingException, SQLException {

        // 1. Đọc dữ liệu từ message
        String toEmail = report.get("toEmail").asText();
        LocalDate reportDate = LocalDate.parse(report.get("reconciliationDate").asText());
        int ourCount = report.get("ourCount").asInt();
        int partnerCount = report.get("partnerCount").asInt();
        int bothHave = report.get("bothHave").asInt();
        int ourOnly = report.get("ourOnly").asInt();
        int partnerOnly = report.get("partnerOnly").asInt();

        // 2. Cấu hình kết nối SMTP
        Properties properties = new Properties();
        properties.put("mail.smtp.host", config.get("mail.smtp.host"));
        properties.put("mail.smtp.port", config.get("mail.smtp.port"));
        properties.put("mail.smtp.auth", "false");
        properties.put("mail.smtp.starttls.enable", "false");

        Session session = Session.getInstance(properties);

        // 3. Tạo nội dung báo cáo
        String htmlContent = """
                <html>
                <body>
                    <h2>Báo cáo đối soát giao dịch QR</h2>
                    <p>Ngày đối soát: <strong>%s</strong></p>

                    <table border="1" cellpadding="8">
                        <tr>
                            <th>Chỉ tiêu</th>
                            <th>Số lượng</th>
                        </tr>
                        <tr>
                            <td>Giao dịch bên mình</td>
                            <td>%d</td>
                        </tr>
                        <tr>
                            <td>Giao dịch đối tác</td>
                            <td>%d</td>
                        </tr>
                        <tr>
                            <td>Cả hai bên đều có</td>
                            <td>%d</td>
                        </tr>
                        <tr>
                            <td>Chỉ bên mình có</td>
                            <td>%d</td>
                        </tr>
                        <tr>
                            <td>Chỉ đối tác có</td>
                            <td>%d</td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(
                reportDate,
                ourCount,
                partnerCount,
                bothHave,
                ourOnly,
                partnerOnly
        );

        // 4. Soạn mail
        MimeMessage message = new MimeMessage(session);

        message.setFrom(
                new InternetAddress(config.get("mail.from"))
        );

        message.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(toEmail)
        );

        message.setSubject(
                "Báo cáo đối soát ngày " + reportDate,
                "UTF-8"
        );

        message.setContent(
                htmlContent,
                "text/html; charset=UTF-8"
        );

        // 5. Gửi mail
        Transport.send(message);
        LocalDateTime sentAt = LocalDateTime.now();
        log.info("SMTP đã chấp nhận gửi báo cáo tới {}", toEmail);


        reportRepository.saveReport(report, sentAt);
        log.info("Đã lưu lịch sử gửi báo cáo vào database");

    }
}