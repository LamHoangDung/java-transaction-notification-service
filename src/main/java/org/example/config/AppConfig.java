package org.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class AppConfig {
    private static final Logger log =
            LoggerFactory.getLogger(AppConfig.class);

    private final Properties properties = new Properties();
    private final Map<String, String> envValues = new HashMap<>();

    public AppConfig() {
        loadProperties();
        loadEnv();
    }

    // Đọc cấu hình trong src/main/resources/app.properties.
    private void loadProperties() {
        try (InputStream input = getClass()
                .getClassLoader()
                .getResourceAsStream("app.properties")) {

            if (input == null) {
                throw new IllegalStateException(
                        "Không tìm thấy app.properties"
                );
            }

            properties.load(input);
            log.info("Đã đọc app.properties");

        } catch (IOException e) {
            throw new RuntimeException(
                    "Không thể đọc app.properties",
                    e
            );
        }
    }

    // Đọc file .env
    private void loadEnv() {
        Path envPath = Path.of(".env").toAbsolutePath();

        // Nếu không có .env, vẫn có thể dùng biến môi trường thật.
        if (!Files.exists(envPath)) {
            log.info(
                    "Không tìm thấy .env tại {}; "
                            + "sẽ sử dụng biến môi trường hệ thống",
                    envPath
            );
            return;
        }
        try {
            int lineNumber = 0;

            for (String line : Files.readAllLines(
                    envPath,
                    StandardCharsets.UTF_8
            )) {
                lineNumber++;

                String content = line.trim();

                // Bỏ qua dòng trống và comment.
                if (content.isEmpty() || content.startsWith("#")) {
                    continue;
                }

                // Tách tại dấu '=' đầu tiên.
                int separator = content.indexOf('=');

                if (separator <= 0) {
                    throw new IllegalArgumentException(
                            "Sai định dạng .env tại dòng " + lineNumber
                    );
                }

                String key =
                        content.substring(0, separator).trim();

                String value =
                        content.substring(separator + 1).trim();

                envValues.put(key, value);
            }

            log.info("Đã đọc .env");

        } catch (IOException e) {
            throw new RuntimeException(
                    "Không thể đọc .env tại " + envPath,
                    e
            );
        }
    }

    public String get(String key) {
        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Thiếu cấu hình trong app.properties: " + key
            );
        }

        value = value.trim();

        // Ví dụ: ${DB_PASSWORD}.
        if (value.startsWith("${") && value.endsWith("}")) {
            String envName =
                    value.substring(2, value.length() - 1);

            // Ưu tiên biến môi trường thật của process Java.
            String resolvedValue = System.getenv(envName);

            // Nếu chưa có, tìm trong file .env.
            if (resolvedValue == null || resolvedValue.isBlank()) {
                resolvedValue = envValues.get(envName);
            }

            if (resolvedValue == null || resolvedValue.isBlank()) {
                throw new IllegalStateException(
                        "Thiếu biến " + envName + " trong môi trường hoặc file .env"
                );
            }

            return resolvedValue;
        }

        // Cấu hình thông thường: trả trực tiếp.
        return value;
    }

    public int getInt(String key) {
        return Integer.parseInt(get(key));
    }
}
