package com.engine.payment.adapters.out.gateway;

import com.engine.payment.domain.exception.PaymentFailedException;
import com.engine.payment.domain.model.CardToken;
import com.engine.payment.domain.model.GatewayReference;
import com.engine.shared.domain.model.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class PaymentGatewayClientTest {

    @Test
    void testAuthorizeThrowsExceptionWhenWebClientFails() {
        WebClient.Builder builderMock = mock(WebClient.Builder.class);
        WebClient webClientMock = mock(WebClient.class);
        when(builderMock.baseUrl(anyString())).thenReturn(builderMock);
        when(builderMock.build()).thenReturn(webClientMock);

        PaymentGatewayClient client = new PaymentGatewayClient(builderMock, "http://localhost");

        WebClient.RequestBodyUriSpec requestBodyUriSpecMock = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpecMock = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpecMock = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpecMock = mock(WebClient.ResponseSpec.class);

        when(webClientMock.post()).thenReturn(requestBodyUriSpecMock);
        when(requestBodyUriSpecMock.uri(anyString())).thenReturn(requestBodySpecMock);
        when(requestBodySpecMock.bodyValue(any())).thenReturn(requestHeadersSpecMock);
        when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpecMock);
        // Simulate a failure:
        when(responseSpecMock.onStatus(any(), any())).thenReturn(responseSpecMock);
        when(responseSpecMock.bodyToMono(PaymentGatewayClient.GatewayResponse.class))
                .thenReturn(Mono.error(new RuntimeException("Connection refused")));

        assertThrows(PaymentFailedException.class, () -> 
            client.authorize(new CardToken("tok_123"), Money.of(100L, Currency.getInstance("USD")))
        );
    }
}
