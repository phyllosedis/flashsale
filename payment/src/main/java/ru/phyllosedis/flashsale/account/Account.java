package ru.phyllosedis.flashsale.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.phyllosedis.flashsale.payment.exceptions.InsufficientFundsException;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Version
    private Long version;

    public Account(UUID userId) {
        this.userId = userId;
    }

    /**
     * Test and bootstrap helper. Business code should use {@link #debit(BigDecimal)} /
     * {@link #credit(BigDecimal)} so that balance invariants stay inside the aggregate.
     */
    public static Account withBalance(UUID userId, BigDecimal initialAmount) {
        Account account = new Account(userId);
        account.amount = initialAmount;
        return account;
    }

    public boolean hasAtLeast(BigDecimal value) {
        return amount.compareTo(value) >= 0;
    }

    public void debit(BigDecimal value) {
        if (!hasAtLeast(value)) {
            throw new InsufficientFundsException("Недостаточно средств на счёте");
        }
        amount = amount.subtract(value);
    }

    public void credit(BigDecimal value) {
        amount = amount.add(value);
    }
}
