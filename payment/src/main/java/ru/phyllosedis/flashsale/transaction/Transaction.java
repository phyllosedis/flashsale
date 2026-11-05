package ru.phyllosedis.flashsale.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Table(name = "\"transaction\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason")
    private TransactionFailedReason reason;

    private Transaction(UUID from, UUID to, BigDecimal amount, TransactionStatus status,
                        TransactionFailedReason reason) {
        this.from = from;
        this.to = to;
        this.amount = amount;
        this.status = status;
        this.reason = reason;
    }

    public static Transaction pending(UUID from, UUID to, BigDecimal amount) {
        return new Transaction(from, to, amount, TransactionStatus.PENDING, null);
    }

    public static Transaction rejected(UUID from, UUID to, BigDecimal amount, TransactionFailedReason reason) {
        return new Transaction(from, to, amount, TransactionStatus.REJECTED, reason);
    }

    public void complete() {
        if (status != TransactionStatus.PENDING) {
            throw new IllegalStateException("Only a pending transaction can be completed, but was " + status);
        }
        status = TransactionStatus.COMPLETED;
        reason = null;
    }

    public void reject(TransactionFailedReason reason) {
        if (status == TransactionStatus.COMPLETED) {
            throw new IllegalStateException("A completed transaction cannot be rejected");
        }
        status = TransactionStatus.REJECTED;
        this.reason = reason;
    }
}
