package com.engine.payment.adapters.in.web;

import com.engine.payment.domain.model.CardToken;
import com.engine.payment.domain.port.in.AuthorizePaymentUseCase;
import com.engine.payment.domain.port.in.CapturePaymentUseCase;
import com.engine.payment.domain.port.in.GetPaymentQuery;
import com.engine.payment.domain.port.in.RefundPaymentUseCase;
import com.engine.shared.domain.ids.OrderId;
import com.engine.shared.domain.ids.PaymentId;
import com.engine.shared.domain.model.Money;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final AuthorizePaymentUseCase authorizeUseCase;
    private final CapturePaymentUseCase captureUseCase;
    private final RefundPaymentUseCase refundUseCase;
    private final GetPaymentQuery getPaymentQuery;
    private final ObjectMapper objectMapper;

    public PaymentController(AuthorizePaymentUseCase authorizeUseCase, CapturePaymentUseCase captureUseCase, RefundPaymentUseCase refundUseCase, GetPaymentQuery getPaymentQuery, ObjectMapper objectMapper) {
        this.authorizeUseCase = authorizeUseCase;
        this.captureUseCase = captureUseCase;
        this.refundUseCase = refundUseCase;
        this.getPaymentQuery = getPaymentQuery;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/authorize")
    public ResponseEntity<Void> authorize(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody PaymentDto.AuthorizePaymentRequest request
    ) throws Exception {
        String hash = DigestUtils.md5DigestAsHex(objectMapper.writeValueAsBytes(request));
        AuthorizePaymentUseCase.AuthorizePaymentCommand command = new AuthorizePaymentUseCase.AuthorizePaymentCommand(
                new PaymentId(request.paymentId()),
                new OrderId(request.orderId()),
                Money.of(request.amount().longValue(), Currency.getInstance(request.currency())),
                new CardToken(request.cardToken()),
                idempotencyKey,
                hash
        );
        authorizeUseCase.authorize(command);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/capture")
    public ResponseEntity<Void> capture(
            @PathVariable UUID id
    ) {
        captureUseCase.capture(new CapturePaymentUseCase.CapturePaymentCommand(new PaymentId(id)));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<Void> refund(
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody PaymentDto.RefundPaymentRequest request
    ) throws Exception {
        String hash = DigestUtils.md5DigestAsHex(objectMapper.writeValueAsBytes(request));
        RefundPaymentUseCase.RefundPaymentCommand command = new RefundPaymentUseCase.RefundPaymentCommand(
                new PaymentId(id),
                Money.of(request.amount().longValue(), Currency.getInstance(request.currency())),
                idempotencyKey,
                hash
        );
        refundUseCase.refund(command);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentDto.PaymentResponse> getPayment(@PathVariable UUID id) {
        GetPaymentQuery.PaymentView view = getPaymentQuery.getPayment(new PaymentId(id));
        return ResponseEntity.ok(new PaymentDto.PaymentResponse(
                view.id().value(),
                view.status().name(),
                view.amount().amount(),
                view.capturedAmount().amount(),
                view.refundedAmount().amount()
        ));
    }
}
