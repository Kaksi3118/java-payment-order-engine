package com.engine.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Singleton {@link Clock} bean on UTC. Injected into every use case and adapter that records
 * timestamps &mdash; user registration, lifecycle transitions, JWT issuance, outbox rows. Tests
 * override this with a fixed clock via {@code @MockBean(Clock.class)} or a {@code @TestConfiguration}
 * {@code @Primary} fixed-clock bean for deterministic time assertions.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}