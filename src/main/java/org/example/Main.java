package org.example;

import org.example.config.AppConfig;
import org.example.config.DatabaseConfig;
import org.example.repository.ReportRepository;
import org.example.service.EmailService;
import org.example.service.RabbitMQConsumerService;

public class Main {

    public static void main(String[] args){

        AppConfig config = new AppConfig();
            DatabaseConfig.init(config);
            ReportRepository reportRepository = new ReportRepository();
            EmailService emailService = new EmailService(config, reportRepository);
            RabbitMQConsumerService rabbitMQConsumerService = new RabbitMQConsumerService(config, emailService);
            rabbitMQConsumerService.start();

    }
}