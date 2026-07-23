package com.engine.order.domain.exception;

import java.util.List;
import java.util.UUID;

/**
 * Thrown when the inventory port reports that one or more products cannot be reserved
 * in the requested quantity. Carries the offending product IDs so the REST layer can
 * surface them to the caller.
 */
public final class InsufficientInventoryException extends RuntimeException {

    private final List<UUID> unavailableProducts;

    public InsufficientInventoryException(List<UUID> unavailableProducts) {
        super("Insufficient inventory for products: " + unavailableProducts);
        this.unavailableProducts = unavailableProducts;
    }

    public List<UUID> unavailableProducts() {
        return unavailableProducts;
    }
}