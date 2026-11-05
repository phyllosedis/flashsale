package ru.phyllosedis.flashsale.payment.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.phyllosedis.flashsale.payment.PaymentResult;
import ru.phyllosedis.flashsale.payment.PaymentService;
import ru.phyllosedis.flashsale.payment.exceptions.IdempotencyConflictException;
import ru.phyllosedis.flashsale.transaction.TransactionFailedReason;
import ru.phyllosedis.flashsale.transaction.TransactionStatus;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    private MockMvc mockMvc;

    private final UUID from = UUID.randomUUID();
    private final UUID to = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        PaymentController controller = new PaymentController(paymentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new PaymentExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Returns 201 with the transaction id for a completed transfer")
    void completedTransferReturnsCreated() throws Exception {
        UUID transactionId = UUID.randomUUID();
        when(paymentService.makeTransaction(any(), any(), any(), eq("order-42")))
                .thenReturn(new PaymentResult(transactionId, TransactionStatus.COMPLETED, null));

        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "order-42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("10.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value(transactionId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.reason").doesNotExist());
    }

    @Test
    @DisplayName("Returns 422 with a reason for a rejected transfer")
    void rejectedTransferReturnsUnprocessableEntity() throws Exception {
        when(paymentService.makeTransaction(any(), any(), any(), any()))
                .thenReturn(new PaymentResult(
                        UUID.randomUUID(), TransactionStatus.REJECTED, TransactionFailedReason.NOT_ENOUGH_MONEY));

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("10.00")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reason").value("NOT_ENOUGH_MONEY"));
    }

    @Test
    @DisplayName("Returns 409 when the idempotency key is still in flight")
    void inFlightIdempotencyKeyReturnsConflict() throws Exception {
        when(paymentService.makeTransaction(any(), any(), any(), eq("order-42")))
                .thenThrow(new IdempotencyConflictException("order-42"));

        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "order-42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("10.00")))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Returns 400 for an invalid request body")
    void invalidBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fromAccountId": null, "toAccountId": null, "amount": -5}
                                """))
                .andExpect(status().isBadRequest());
    }

    private String body(String amount) {
        return """
                {"fromAccountId": "%s", "toAccountId": "%s", "amount": %s}
                """.formatted(from, to, amount);
    }
}
