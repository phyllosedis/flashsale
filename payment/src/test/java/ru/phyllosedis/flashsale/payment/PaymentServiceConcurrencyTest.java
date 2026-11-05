package ru.phyllosedis.flashsale.payment;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.phyllosedis.flashsale.account.Account;
import ru.phyllosedis.flashsale.account.AccountRepository;
import ru.phyllosedis.flashsale.transaction.Transaction;
import ru.phyllosedis.flashsale.transaction.TransactionFailedReason;
import ru.phyllosedis.flashsale.transaction.TransactionRepository;
import ru.phyllosedis.flashsale.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PaymentServiceConcurrencyTest {

    private static final int THREAD_POOL_SIZE = 10;
    private static final long TASK_TIMEOUT_SECONDS = 30;

    @Autowired
    private PaymentService paymentService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID accountAlphaId;
    private UUID accountBetaId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM payment_idempotency");
        transactionRepository.deleteAll();
        accountRepository.deleteAll();

        Account alpha = Account.withBalance(UUID.randomUUID(), BigDecimal.valueOf(1000));
        accountRepository.save(alpha);
        accountAlphaId = alpha.getId();

        Account beta = Account.withBalance(UUID.randomUUID(), BigDecimal.valueOf(1000));
        accountRepository.save(beta);
        accountBetaId = beta.getId();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM payment_idempotency");
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    @DisplayName("Concurrent transfers in both directions keep the total balance stable")
    void positiveConcurrencyTest() throws Exception {
        int transfersCount = 100;

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < transfersCount; i++) {
            final int index = i;
            tasks.add(() -> {
                if (index % 2 == 0) {
                    paymentService.makeTransaction(accountAlphaId, accountBetaId, BigDecimal.TEN);
                } else {
                    paymentService.makeTransaction(accountBetaId, accountAlphaId, BigDecimal.TEN);
                }
                return null;
            });
        }

        runConcurrently(tasks);

        Account alpha = accountRepository.findById(accountAlphaId).orElseThrow();
        Account beta = accountRepository.findById(accountBetaId).orElseThrow();

        assertThat(alpha.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(beta.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));

        long completedCount = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getStatus() == TransactionStatus.COMPLETED)
                .count();
        assertThat(completedCount).isEqualTo(transfersCount);
    }

    @Test
    @DisplayName("Concurrent transfers never overdraw an account and persist rejections")
    void negativeNotEnoughMoney() throws Exception {
        int transfersCount = 5;
        BigDecimal amount = BigDecimal.valueOf(300);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < transfersCount; i++) {
            tasks.add(() -> {
                paymentService.makeTransaction(accountAlphaId, accountBetaId, amount);
                return null;
            });
        }

        runConcurrently(tasks);

        Account alpha = accountRepository.findById(accountAlphaId).orElseThrow();
        Account beta = accountRepository.findById(accountBetaId).orElseThrow();

        assertThat(alpha.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(beta.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1900));

        List<Transaction> transactions = transactionRepository.findAll();
        List<Transaction> completed = transactions.stream()
                .filter(transaction -> transaction.getStatus() == TransactionStatus.COMPLETED)
                .toList();
        List<Transaction> rejected = transactions.stream()
                .filter(transaction -> transaction.getStatus() == TransactionStatus.REJECTED)
                .toList();

        assertThat(completed).hasSize(3);
        assertThat(rejected).hasSize(2);
        assertThat(rejected).allMatch(transaction ->
                transaction.getReason() == TransactionFailedReason.NOT_ENOUGH_MONEY);
    }

    @Test
    @DisplayName("A replayed idempotency key moves money only once")
    void idempotentReplayMovesMoneyOnce() {
        String key = UUID.randomUUID().toString();

        PaymentResult first = paymentService.makeTransaction(
                accountAlphaId, accountBetaId, BigDecimal.TEN, key);
        PaymentResult replay = paymentService.makeTransaction(
                accountAlphaId, accountBetaId, BigDecimal.TEN, key);

        assertThat(replay.transactionId()).isEqualTo(first.transactionId());
        assertThat(accountRepository.findById(accountAlphaId).orElseThrow().getAmount())
                .isEqualByComparingTo(BigDecimal.valueOf(990));
        assertThat(accountRepository.findById(accountBetaId).orElseThrow().getAmount())
                .isEqualByComparingTo(BigDecimal.valueOf(1010));
        assertThat(transactionRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("Negative amount is rejected and does not change balances")
    void transferNegativeValue() {
        paymentService.makeTransaction(accountAlphaId, accountBetaId, new BigDecimal("-100"));

        Account alpha = accountRepository.findById(accountAlphaId).orElseThrow();
        assertThat(alpha.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    /**
     * Starts every task at the same moment and fails the test if any of them completes
     * exceptionally or hangs. The previous version ignored {@link Future} results, which
     * hid rollback-only failures and made the test green for the wrong reason.
     */
    private void runConcurrently(List<? extends Callable<?>> tasks) throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(tasks.size(), THREAD_POOL_SIZE))) {
            List<Future<?>> futures = new ArrayList<>(tasks.size());
            for (Callable<?> task : tasks) {
                futures.add(executor.submit(() -> {
                    startLatch.await();
                    return task.call();
                }));
            }

            startLatch.countDown();

            for (Future<?> future : futures) {
                future.get(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
        }
    }
}
