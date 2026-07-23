package com.engine.identity.adapters.out.persistence;

/**
 * Lifecycle status of an outbox row.
 *
 * <ul>
 *     <li>{@link #PENDING} &mdash; written in-transaction, awaiting the asynchronous poller.</li>
 *     <li>{@link #PUBLISHED} &mdash; the poller successfully published to RabbitMQ.</li>
 *     <li>{@link #FAILED} &mdash; exhausted retries; routed to the DLQ for manual intervention.</li>
 * </ul>
 */
public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}