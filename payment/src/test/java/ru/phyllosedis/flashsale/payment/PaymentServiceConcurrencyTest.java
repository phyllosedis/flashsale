package ru.phyllosedis.flashsale.payment;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.phyllosedis.flashsale.account.Account;
import ru.phyllosedis.flashsale.account.AccountRepository;
import ru.phyllosedis.flashsale.transaction.Transaction;
import ru.phyllosedis.flashsale.transaction.TransactionFailedReason;
import ru.phyllosedis.flashsale.transaction.TransactionRepository;
import ru.phyllosedis.flashsale.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
public class PaymentServiceConcurrencyTest {
    @Autowired
    PaymentService paymentService;
    @Autowired
    AccountRepository accountRepository;
    @Autowired
    TransactionRepository transactionRepository;

    private UUID accountAlphaId;
    private UUID accountBetaId;

    @BeforeEach
    void beforeEach() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();

        Account alpha = new Account();
        alpha.setUser(UUID.randomUUID());
        alpha.setAmount(BigDecimal.valueOf(1000));
        accountRepository.save(alpha);
        accountAlphaId = alpha.getId();

        Account beta = new Account();
        beta.setUser(UUID.randomUUID());
        beta.setAmount(BigDecimal.valueOf(1000));
        accountRepository.save(beta);
        accountBetaId = beta.getId();
    }

    @AfterEach
    void afterEach() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    @DisplayName("Позитивный тест: Встречные переводы не вызывают дедлоков, баланс сходится")
    void positiveConcurrencyTest() throws InterruptedException {
        int threadsCount = 100;
        try (ExecutorService executorService = Executors.newFixedThreadPool(10)) {

            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch finishLatch = new CountDownLatch(threadsCount);

            for (int i = 0; i < threadsCount; i++) {
                final int index = i;
                executorService.submit(() -> {
                    try {
                        startLatch.await();
                        if (index % 2 == 0) {
                            paymentService.makeTransaction(accountAlphaId, accountBetaId, BigDecimal.TEN);
                        } else {
                            paymentService.makeTransaction(accountBetaId, accountAlphaId, BigDecimal.TEN);
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } finally {
                        finishLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            finishLatch.await();
        }

        Account alphaAccountAccount = accountRepository.findById(accountAlphaId).orElseThrow();
        Account betaAccountAccount = accountRepository.findById(accountBetaId).orElseThrow();

        Assertions.assertEquals(0, alphaAccountAccount.getAmount().compareTo(BigDecimal.valueOf(1000)));
        Assertions.assertEquals(0, betaAccountAccount.getAmount().compareTo(BigDecimal.valueOf(1000)));

        long completedCount = transactionRepository.findAll().stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .count();
        Assertions.assertEquals(threadsCount, completedCount);
    }

    @Test
    @DisplayName("Негативный тест: Попытка перевести больше денег, чем есть на самом деле")
    void negativeNotEnoughMoney() throws InterruptedException {
        // первые переводы сделаем успешные, последующие неуспешные
        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        BigDecimal amount = BigDecimal.valueOf(300L);

        try (ExecutorService executorService = Executors.newFixedThreadPool(threadCount)) {
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        startLatch.await();
                        paymentService.makeTransaction(accountAlphaId, accountBetaId, amount);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } finally {
                        finishLatch.countDown();
                    }
                });
            }
            startLatch.countDown();
            finishLatch.await();
        }
        Account alpha = accountRepository.findById(accountAlphaId).orElseThrow();
        Account beta = accountRepository.findById(accountBetaId).orElseThrow();

        Assertions.assertEquals(0, alpha.getAmount().compareTo(BigDecimal.valueOf(100L)));
        Assertions.assertEquals(0, beta.getAmount().compareTo(BigDecimal.valueOf(1900L)));

        List<Transaction> transactions = transactionRepository.findAll();

        List<Transaction> rejected = transactions.stream()
                .filter(e -> e.getStatus().equals(TransactionStatus.REJECTED))
                .toList();
        List<Transaction> completed = transactions.stream()
                .filter(e -> e.getStatus().equals(TransactionStatus.COMPLETED))
                .toList();

        Assertions.assertEquals(3, completed.size());
        Assertions.assertEquals(2, rejected.size());

        long countRejectedByReason = rejected.stream()
                .map(Transaction::getReason)
                .filter(e -> e.equals(TransactionFailedReason.NOT_ENOUGH_MONEY))
                .count();
        Assertions.assertEquals(2, countRejectedByReason);
    }
}
