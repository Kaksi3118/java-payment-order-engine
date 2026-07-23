package com.engine.payment.adapters.in.web;

import com.engine.payment.domain.port.in.AuthorizePaymentUseCase;
import com.engine.payment.domain.port.in.CapturePaymentUseCase;
import com.engine.payment.domain.port.in.GetPaymentQuery;
import com.engine.payment.domain.port.in.RefundPaymentUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@ContextConfiguration(classes = {PaymentController.class, PaymentExceptionHandler.class})
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthorizePaymentUseCase authorizeUseCase;

    @MockBean
    private CapturePaymentUseCase captureUseCase;

    @MockBean
    private RefundPaymentUseCase refundUseCase;

    @MockBean
    private GetPaymentQuery getPaymentQuery;

    @Test
    void testAuthorizeEndpoint() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentDto.AuthorizePaymentRequest req = new PaymentDto.AuthorizePaymentRequest(
                paymentId, orderId, new BigDecimal("100.00"), "USD", "tok_visa"
        );

        mockMvc.perform(post("/api/payments/authorize")
                        .header("Idempotency-Key", "key-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(authorizeUseCase).authorize(any(AuthorizePaymentUseCase.AuthorizePaymentCommand.class));
    }
}
