package com.engine.payment.domain.port.out;

import java.util.Optional;

public interface IdempotencyPort {
    Optional<String> findResult(String key, String requestHash);
    void saveResult(String key, String requestHash, String resultJson);
}
