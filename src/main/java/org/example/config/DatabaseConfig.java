package org.example.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);
    private static HikariDataSource dataSource;

    // khoi tao poop ket noi
    public static void init(AppConfig config) {
        HikariConfig hikariConfig = new HikariConfig();

        // 1. Lấy thông tin từ AppConfig
        hikariConfig.setJdbcUrl(config.get("db.url"));
        hikariConfig.setUsername(config.get("db.username"));
        hikariConfig.setPassword(config.get("db.password"));

        // 2. Cấu hình số kết nối tối đa
        hikariConfig.setMaximumPoolSize(config.getInt("db.pool.size"));
        hikariConfig.setPoolName("QR-Reconciliation-Pool");

        // 3. Thời gian timeout (milliseconds)
        hikariConfig.setConnectionTimeout(30_000);
        hikariConfig.setIdleTimeout(600_000);

        // Khởi động pool
        dataSource = new HikariDataSource(hikariConfig);
        log.info("ket noi thanh cong");
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new IllegalStateException("databaseconfig chua duoc khoi tao, phai intit() truoc");
        }
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("Đã đóng Database pool.");
        }
    }
}