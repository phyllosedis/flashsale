package ru.phyllosedis.flashsale.transaction;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

@Table(name = "\"transaction\"")
@Entity
@Getter
@Setter
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @CreationTimestamp
    private Timestamp createdAt;

    @Column(name = "account_from", nullable = false)
    private UUID from;

    @Column(name = "account_to", nullable = false)
    private UUID to;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    private TransactionFailedReason reason;
}
