package com.engine.order.adapters.in.rest;

import com.engine.order.adapters.in.rest.dto.OrderResponse;
import com.engine.order.adapters.in.rest.dto.PlaceOrderRequest;
import com.engine.order.adapters.in.rest.dto.PlaceOrderResponse;
import com.engine.order.domain.model.OrderItem;
import com.engine.order.domain.model.OrderView;
import com.engine.order.domain.port.in.CancelOrderUseCase;
import com.engine.order.domain.port.in.GetOrderQuery;
import com.engine.order.domain.port.in.PlaceOrderUseCase;
import com.engine.order.domain.port.in.PlaceOrderUseCase.PlaceOrderCommand;
import com.engine.order.domain.port.in.PlaceOrderUseCase.PlaceOrderResult;
import com.engine.shared.domain.ids.OrderId;
import com.engine.shared.domain.ids.UserId;
import com.engine.shared.domain.model.Money;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * REST adapter: order endpoints.
 *
 * <p>Three endpoints:
 * <ul>
 *     <li>{@code POST /api/orders} &mdash; place a new order; requires {@code Idempotency-Key} header.</li>
 *     <li>{@code POST /api/orders/{id}/cancel} &mdash; cancel an order; requires {@code Idempotency-Key} header.</li>
 *     <li>{@code GET /api/orders/{id}} &mdash; retrieve an order (CQRS read side).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final PlaceOrderUseCase placeOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final GetOrderQuery getOrderQuery;

    public OrderController(PlaceOrderUseCase placeOrderUseCase,
                           CancelOrderUseCase cancelOrderUseCase,
                           GetOrderQuery getOrderQuery) {
        this.placeOrderUseCase = placeOrderUseCase;
        this.cancelOrderUseCase = cancelOrderUseCase;
        this.getOrderQuery = getOrderQuery;
    }

    @PostMapping
    public ResponseEntity<PlaceOrderResponse> placeOrder(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PlaceOrderRequest request) {

        List<OrderItem> items = request.items().stream()
                .map(li -> new OrderItem(
                        UUID.fromString(li.productId()),
                        li.quantity(),
                        Money.of(li.unitPrice(), li.currency())))
                .toList();

        String requestHash = hashRequest(request);

        PlaceOrderCommand command = new PlaceOrderCommand(
                UserId.of(UUID.fromString(request.customerId())),
                items,
                idempotencyKey,
                requestHash);

        PlaceOrderResult result = placeOrderUseCase.place(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new PlaceOrderResponse(result.orderId()));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable("id") String orderId,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        String requestHash = hashString("cancel:" + orderId);
        cancelOrderUseCase.cancel(new CancelOrderUseCase.CancelOrderCommand(
                OrderId.of(UUID.fromString(orderId)),
                idempotencyKey,
                requestHash));

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable("id") String orderId) {
        return getOrderQuery.findById(OrderId.of(UUID.fromString(orderId)))
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private OrderResponse toResponse(OrderView view) {
        List<OrderResponse.LineItem> items = view.items().stream()
                .map(i -> new OrderResponse.LineItem(
                        i.productId(), i.quantity(),
                        i.unitPrice().amount(), i.unitPrice().currency().getCurrencyCode()))
                .toList();
        return new OrderResponse(
                view.orderId(), view.customerId(), view.status(),
                view.currency().getCurrencyCode(), items,
                view.totalAmount().amount(), view.createdAt(), view.updatedAt());
    }

    private static String hashRequest(PlaceOrderRequest request) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(request.toString().getBytes());
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String hashString(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(input.getBytes());
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}