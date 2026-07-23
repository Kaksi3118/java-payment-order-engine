package com.engine.payment.adapters.out.gateway;

import com.engine.payment.domain.exception.PaymentFailedException;
import com.engine.payment.domain.model.CardToken;
import com.engine.payment.domain.model.GatewayReference;
import com.engine.payment.domain.port.out.PaymentGatewayPort;
import com.engine.shared.domain.model.Money;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;

@Component
public class PaymentGatewayClient implements PaymentGatewayPort {

    private final WebClient webClient;
    private final Timer latencyTimer;
    private final Counter callsCounter;

    public PaymentGatewayClient(WebClient.Builder webClientBuilder, @Value("${payment.gateway.url:http://localhost:9090}") String gatewayUrl, MeterRegistry meterRegistry) {
        this.webClient = webClientBuilder.baseUrl(gatewayUrl).build();
        this.latencyTimer = meterRegistry.timer("payment.gateway.latency");
        this.callsCounter = meterRegistry.counter("payment.gateway.calls");
    }

    @Override
    @CircuitBreaker(name = "payment-gateway")
    @RateLimiter(name = "payment-gateway")
    @Bulkhead(name = "payment-gateway")
    @Retry(name = "payment-gateway")
    public GatewayReference authorize(CardToken token, Money amount) {
        callsCounter.increment();
        return latencyTimer.record(() -> {
            try {
                GatewayResponse response = webClient.post()
                        .uri("/v1/authorize")
                        .bodyValue(new AuthorizeRequest(token.value(), amount.amount().longValue(), amount.currency().getCurrencyCode()))
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, res -> Mono.error(new PaymentFailedException("Gateway Error: " + res.statusCode())))
                        .bodyToMono(GatewayResponse.class)
                        .block();
                if (response == null || response.reference() == null) {
                     throw new PaymentFailedException("Invalid gateway response");
                }
                return new GatewayReference(response.reference());
            } catch (Exception e) {
                throw new PaymentFailedException(e.getMessage());
            }
        });
    }

    @Override
    @CircuitBreaker(name = "payment-gateway")
    @RateLimiter(name = "payment-gateway")
    @Bulkhead(name = "payment-gateway")
    @Retry(name = "payment-gateway")
    public GatewayReference capture(GatewayReference authorizationReference, Money amount) {
        callsCounter.increment();
        return latencyTimer.record(() -> {
            try {
                GatewayResponse response = webClient.post()
                        .uri("/v1/capture")
                        .bodyValue(new CaptureRequest(authorizationReference.value(), amount.amount().longValue(), amount.currency().getCurrencyCode()))
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, res -> Mono.error(new PaymentFailedException("Gateway Error: " + res.statusCode())))
                        .bodyToMono(GatewayResponse.class)
                        .block();
                if (response == null || response.reference() == null) {
                     throw new PaymentFailedException("Invalid gateway response");
                }
                return new GatewayReference(response.reference());
            } catch (Exception e) {
                throw new PaymentFailedException(e.getMessage());
            }
        });
    }

    @Override
    @CircuitBreaker(name = "payment-gateway")
    @RateLimiter(name = "payment-gateway")
    @Bulkhead(name = "payment-gateway")
    @Retry(name = "payment-gateway")
    public GatewayReference refund(GatewayReference captureReference, Money amount) {
        callsCounter.increment();
        return latencyTimer.record(() -> {
            try {
                GatewayResponse response = webClient.post()
                        .uri("/v1/refund")
                        .bodyValue(new RefundRequest(captureReference.value(), amount.amount().longValue(), amount.currency().getCurrencyCode()))
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, res -> Mono.error(new PaymentFailedException("Gateway Error: " + res.statusCode())))
                        .bodyToMono(GatewayResponse.class)
                        .block();
                if (response == null || response.reference() == null) {
                     throw new PaymentFailedException("Invalid gateway response");
                }
                return new GatewayReference(response.reference());
            } catch (Exception e) {
                throw new PaymentFailedException(e.getMessage());
            }
        });
    }

    @Override
    @CircuitBreaker(name = "payment-gateway")
    @RateLimiter(name = "payment-gateway")
    @Bulkhead(name = "payment-gateway")
    @Retry(name = "payment-gateway")
    public void voidPayment(GatewayReference authorizationReference) {
        callsCounter.increment();
        latencyTimer.record(() -> {
            try {
                webClient.post()
                        .uri("/v1/void")
                        .bodyValue(new VoidRequest(authorizationReference.value()))
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, res -> Mono.error(new PaymentFailedException("Gateway Error: " + res.statusCode())))
                        .bodyToMono(Void.class)
                        .block();
            } catch (Exception e) {
                throw new PaymentFailedException(e.getMessage());
            }
        });
    }

    record AuthorizeRequest(String token, long amount, String currency) {}
    record CaptureRequest(String reference, long amount, String currency) {}
    record RefundRequest(String reference, long amount, String currency) {}
    record VoidRequest(String reference) {}
    record GatewayResponse(String reference, String status) {}
}
