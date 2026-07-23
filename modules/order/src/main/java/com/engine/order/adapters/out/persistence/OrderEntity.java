package com.engine.order.adapters.out.persistence;

import com.engine.order.domain.model.OrderStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for the {@code orders} table. Maps the {@link com.engine.order.domain.model.Order}
 * aggregate's state to a relational row.
 *
 * <p>{@link Version} provides JPA optimistic locking: concurrent modifications to the same
 * order fail with {@code OptimisticLockException}, which the REST layer maps to HTTP 409.
 */
@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "product_id", nullable = false)
    private List<UUID> productIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "quantity", nullable = false)
    private List<Integer> quantities = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private List<BigDecimal> unitPrices = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "currency", nullable = false)
    private List<String> currencies = new ArrayList<>();

    @Column(name = "order_currency", nullable = false)
    private String orderCurrency;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected OrderEntity() {
    }

    public OrderEntity(UUID id, UUID customerId, List<UUID> productIds, List<Integer> quantities,
                       List<BigDecimal> unitPrices, List<String> currencies, String orderCurrency,
                       OrderStatus status, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.customerId = Objects.requireNonNull(customerId, "customerId must not be null");
        this.productIds = new ArrayList<>(Objects.requireNonNull(productIds, "productIds must not be null"));
        this.quantities = new ArrayList<>(Objects.requireNonNull(quantities, "quantities must not be null"));
        this.unitPrices = new ArrayList<>(Objects.requireNonNull(unitPrices, "unitPrices must not be null"));
        this.currencies = new ArrayList<>(Objects.requireNonNull(currencies, "currencies must not be null"));
        this.orderCurrency = Objects.requireNonNull(orderCurrency, "orderCurrency must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public UUID getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public List<UUID> getProductIds() { return productIds; }
    public List<Integer> getQuantities() { return quantities; }
    public List<BigDecimal> getUnitPrices() { return unitPrices; }
    public List<String> getCurrencies() { return currencies; }
    public String getOrderCurrency() { return orderCurrency; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    public void setStatus(OrderStatus status) { this.status = status; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}