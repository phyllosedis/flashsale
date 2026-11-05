package ru.phyllosedis.flashsale.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.phyllosedis.flashsale.transaction.Transaction;
import ru.phyllosedis.flashsale.transaction.TransactionFailedReason;
import ru.phyllosedis.flashsale.transaction.TransactionRepository;
import ru.phyllosedis.flashsale.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private MoneyTransferService moneyTransferService;

    private PaymentService paymentService;

    private final UUID accountFrom = UUID.randomUUID();
    private final UUID accountTo = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(transactionRepository, moneyTransferService);
        lenient().when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Negative amount is rejected without touching accounts")
    void negativeAmountIsRejected() {
        paymentService.makeTransaction(accountFrom, accountTo, new BigDecimal("-0.01"));

        Transaction saved = lastSavedTransaction();
        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.REJECTED);
        assertThat(saved.getReason()).isEqualTo(TransactionFailedReason.INVALID_MONEY_VALUE);
        verifyNoInteractions(moneyTransferService);
    }

    @Test
    @DisplayName("Zero amount is rejected")
    void zeroAmountIsRejected() {
        paymentService.makeTransaction(accountFrom, accountTo, BigDecimal.ZERO);

        assertThat(lastSavedTransaction().getReason()).isEqualTo(TransactionFailedReason.INVALID_MONEY_VALUE);
        verifyNoInteractions(moneyTransferService);
    }

    @Test
    @DisplayName("Amount with more than two decimal digits is rejected")
    void tooPreciseAmountIsRejected() {
        paymentService.makeTransaction(accountFrom, accountTo, new BigDecimal("10.999"));

        assertThat(lastSavedTransaction().getReason()).isEqualTo(TransactionFailedReason.INVALID_MONEY_VALUE);
        verifyNoInteractions(moneyTransferService);
    }

    @Test
    @DisplayName("Transfer to the same account is rejected")
    void sameAccountIsRejected() {
        paymentService.makeTransaction(accountFrom, accountFrom, BigDecimal.TEN);

        assertThat(lastSavedTransaction().getReason()).isEqualTo(TransactionFailedReason.SAME_ACCOUNT);
        verifyNoInteractions(moneyTransferService);
    }

    @Test
    @DisplayName("Null amount is a programming error")
    void nullAmountFailsFast() {
        assertThatThrownBy(() -> paymentService.makeTransaction(accountFrom, accountTo, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("amount");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Insufficient funds reject the transaction instead of failing the request")
    void insufficientFundsRejectTransaction() {
        when(moneyTransferService.executeTransfer(accountFrom, accountTo, BigDecimal.TEN))
                .thenReturn(false);

        paymentService.makeTransaction(accountFrom, accountTo, BigDecimal.TEN);

        Transaction saved = lastSavedTransaction();
        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.REJECTED);
        assertThat(saved.getReason()).isEqualTo(TransactionFailedReason.NOT_ENOUGH_MONEY);
    }

    @Test
    @DisplayName("Successful transfer completes the transaction")
    void successfulTransferCompletesTransaction() {
        when(moneyTransferService.executeTransfer(accountFrom, accountTo, BigDecimal.TEN))
                .thenReturn(true);

        paymentService.makeTransaction(accountFrom, accountTo, BigDecimal.TEN);

        Transaction saved = lastSavedTransaction();
        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(saved.getReason()).isNull();
        verify(moneyTransferService).executeTransfer(accountFrom, accountTo, BigDecimal.TEN);
    }

    private Transaction lastSavedTransaction() {
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        return captor.getValue();
    }
}
