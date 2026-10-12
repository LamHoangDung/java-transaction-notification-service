package org.example.repository;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReportRepository {

    public void saveReport(JsonNode report, LocalDateTime sentAt) throws SQLException {

        String sql = """
                INSERT INTO reconciliation_reports
                    (report_date, both_have, partner_only,
                     our_only, sent_to, sent_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            LocalDate reportDate = LocalDate.parse(
                    report.get("reconciliationDate").asText()
            );

            statement.setDate(1, Date.valueOf(reportDate));
            statement.setInt(2, report.get("bothHave").asInt());
            statement.setInt(3, report.get("partnerOnly").asInt());
            statement.setInt(4, report.get("ourOnly").asInt());
            statement.setString(5, report.get("toEmail").asText());
            statement.setTimestamp(6, Timestamp.valueOf(sentAt));

            statement.executeUpdate();
        }
    }
}