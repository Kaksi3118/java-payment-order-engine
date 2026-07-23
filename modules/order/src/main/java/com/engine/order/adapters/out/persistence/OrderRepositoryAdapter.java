package com.engine.order.adapters.out.persistence;

import com.engine.order.domain.model.Order;
import com.engine.order.domain.model.OrderItem;
import com.engine.order.domain.model.OrderStatus;
import com.engine.order.domain.port.out.OrderRepository;
import com.engine.shared.domain.ids.OrderId;
import com.engine.shared.domain.ids.UserId;
import com.engine.shared.domain.model.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter: implements the {@link OrderRepository} driven port via Spring Data JPA.
 *
 * <p>Handles bidirectional mapping between the domain {@link Order} aggregate and the
 * {@link OrderEntity} JPA entity. Uses load-then-update for existing entities to preserve
 * the JPA {@code @Version} for optimistic locking.
 */
@Component
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepository;

    public OrderRepositoryAdapter(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Order save(Order order) {
        Optional<OrderEntity> existing = jpaRepository.findById(order.idValue());
        OrderEntity entity = existing
                .map(e -> updateExisting(e, order))
                .orElseGet(() -> toNewEntity(order));
        OrderEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return jpaRepository.findById(id.value()).map(this::toDomain);
    }

    private OrderEntity toNewEntity(Order order) {
        List<OrderItem> items = order.items();
        List<UUID> productIds = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();
        List<BigDecimal> unitPrices = new ArrayList<>();
        List<String> currencies = new ArrayList<>();
        for (OrderItem item : items) {
            productIds.add(item.productId());
            quantities.add(item.quantity());
            unitPrices.add(item.unitPrice().amount());
            currencies.add(item.unitPrice().currency().getCurrencyCode());
        }
        return new OrderEntity(
                order.idValue(), order.customerIdValue(),
                productIds, quantities, unitPrices, currencies,
                order.currency().getCurrencyCode(),
                order.status(), order.createdAt(), order.updatedAt());
    }

    private OrderEntity updateExisting(OrderEntity entity, Order order) {
        entity.setStatus(order.status());
        entity.setUpdatedAt(order.updatedAt());
        return entity;
    }

    private Order toDomain(OrderEntity entity) {
        List<OrderItem> items = new ArrayList<>();
        for (int i = 0; i < entity.getProductIds().size(); i++) {
            items.add(new OrderItem(
                    entity.getProductIds().get(i),
                    entity.getQuantities().get(i),
                    Money.of(entity.getUnitPrices().get(i),
                            Currency.getInstance(entity.getCurrencies().get(i)))));
        }
        return Order.reconstitute(
                OrderId.of(entity.getId()),
                UserId.of(entity.getCustomerId()),
                items,
                Currency.getInstance(entity.getOrderCurrency()),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}