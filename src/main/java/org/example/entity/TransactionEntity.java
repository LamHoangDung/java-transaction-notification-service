package org.example.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
public class TransactionEntity {

    private String transactionId;
    private String transactionName;
    private LocalDateTime transactionTime;
    private String status;
    private String notiStatus;

}