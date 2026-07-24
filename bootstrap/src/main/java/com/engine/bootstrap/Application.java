package com.engine.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Application entry point for the Payment &amp; Order Processing Engine.
 *
 * <p>This module wires all bounded contexts (identity, order, payment) and their
 * adapters into a runnable Spring Boot jar. Virtual threads are enabled via
 * {@code spring.threads.virtual.enabled} in {@code application.yml} so that
 * blocking I/O across the request path is carried on carrier threads, dramatically
 * increasing throughput under high concurrency without sacrificing the imperative
 * programming model.
 */
@SpringBootApplication(scanBasePackages = "com.engine")
@EntityScan("com.engine")
@EnableJpaRepositories("com.engine")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}