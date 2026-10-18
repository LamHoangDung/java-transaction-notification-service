package org.example.repository;

import org.example.config.DatabaseConfig;
import org.example.entity.TransactionEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TransactionRepository {

    public TransactionEntity findByTransactionId(String transactionId){
        String sql = """
                SELECT status,
                       noti_status  
                FROM transactions
                WHERE transaction_id = ?
                """;
        try(Connection connection = DatabaseConfig.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, transactionId);
            ResultSet resultSet = statement.executeQuery();
            TransactionEntity transactionEntity = new TransactionEntity();
            if(resultSet.next()) {
               transactionEntity.setNotiStatus(resultSet.getString("noti_status"));
               transactionEntity.setStatus(resultSet.getString("status"));
               return transactionEntity;
            }
        } catch (Exception e) {
            System.err.println("khong tim duoc giao dich: " +e.getMessage());
        }
        return null;
    }
    public int markNotificationSuccess(String transactionId)
            throws SQLException {

        String sql = """
                UPDATE transactions
                SET noti_status = 'SUCCESS'
                WHERE transaction_id = ?
                  AND status = 'SUCCESS'
                  AND noti_status = 'PENDING'
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, transactionId);
            return statement.executeUpdate();
        }
    }
}