package com.engine.identity.adapters.out.persistence;

import com.engine.identity.domain.event.UserRegistered;
import com.engine.identity.domain.model.Email;
import com.engine.identity.domain.model.Role;
import com.engine.identity.domain.model.Roles;
import com.engine.shared.domain.event.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("OutboxAdapter")
class OutboxAdapterTest {

    private OutboxJpaRepository jpaRepository;
    private ObjectMapper objectMapper;
    private OutboxAdapter adapter;

    @BeforeEach
    void setUp() {
        jpaRepository = mock(OutboxJpaRepository.class);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        Clock clock = Clock.fixed(Instant.parse("2026-07-23T12:00:00Z"), ZoneOffset.UTC);
        adapter = new OutboxAdapter(jpaRepository, objectMapper, clock);
    }

    @Test
    @DisplayName("append serializes the event to JSON and persists an OutboxEntity with PENDING status")
    void appendSerializesAndPersists() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-07-23T12:00:00Z");

        DomainEvent event = new UserRegistered(
                eventId, occurredAt, aggregateId,
                Email.of("alice@example.com"),
                Roles.of(Role.CUSTOMER));

        adapter.append(event);

        verify(jpaRepository).save(argThat(entity -> {
            assertThat(entity.getId()).isEqualTo(eventId);
            assertThat(entity.getAggregateId()).isEqualTo(aggregateId);
            assertThat(entity.getEventType()).isEqualTo(UserRegistered.class.getName());
            assertThat(entity.getStatus()).isEqualTo(OutboxStatus.PENDING);
            assertThat(entity.getOccurredAt()).isEqualTo(occurredAt);
            assertThat(entity.getCreatedAt()).isEqualTo(Instant.parse("2026-07-23T12:00:00Z"));
            assertThat(entity.getRetryCount()).isZero();
            assertThat(entity.getPayload()).contains("alice@example.com");
            assertThat(entity.getPayload()).contains("CUSTOMER");
            return true;
        }));
    }

    @Test
    @DisplayName("append rejects null event")
    void rejectsNullEvent() {
        assertThatThrownBy(() -> adapter.append(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("constructor rejects null dependencies")
    void constructorRejectsNulls() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        Clock clock = Clock.systemUTC();

        assertThatThrownBy(() -> new OutboxAdapter(null, mapper, clock))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OutboxAdapter(jpaRepository, null, clock))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OutboxAdapter(jpaRepository, mapper, null))
                .isInstanceOf(NullPointerException.class);
    }
}