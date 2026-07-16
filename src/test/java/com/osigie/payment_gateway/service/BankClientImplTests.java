package com.osigie.payment_gateway.service;

import com.osigie.payment_gateway.domain.bank.*;
import com.osigie.payment_gateway.exception.BankBusinessException;
import com.osigie.payment_gateway.exception.BankUnavailableException;
import com.osigie.payment_gateway.service.impl.BankClientImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BankClientImplTests {

    private MockRestServiceServer mockServer;
    private BankClientImpl bankClient;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        bankClient = new BankClientImpl(restClient);
    }

    @Test
    void authorize_withSuccessfulResponse_shouldReturnAuthorizeBankResponse() {
        var responseJson = """
                {
                    "amount": 5000,
                    "authorization_id": "auth-123",
                    "created_at": "2026-07-16T00:00:00Z",
                    "expires_at": "2026-07-17T00:00:00Z",
                    "status": "SUCCESS"
                }
                """;

        mockServer.expect(requestTo("/authorizations"))
                .andExpect(header("Idempotency-Key", "idem-key"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        var response = bankClient.authorize(
                new AuthorizeBankRequest(5000L, "4111111111111111", "123", 12, 2028),
                "idem-key"
        );

        assertThat(response.amount()).isEqualTo(5000L);
        assertThat(response.authorizationId()).isEqualTo("auth-123");
        assertThat(response.status()).isEqualTo("SUCCESS");
        mockServer.verify();
    }

    @Test
    void authorize_with4xxError_shouldThrowBankBusinessException() {
        var errorJson = """
                {"error": "insufficient_funds", "message": "Insufficient funds"}
                """;

        mockServer.expect(requestTo("/authorizations"))
                .andRespond(withStatus(HttpStatus.PAYMENT_REQUIRED)
                        .body(errorJson)
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() ->
                bankClient.authorize(
                        new AuthorizeBankRequest(5000L, "4111111111111111", "123", 12, 2028),
                        "idem-key"
                )
        )
                .isInstanceOf(BankBusinessException.class)
                .hasMessage("Insufficient funds");
    }

    @Test
    void authorize_with5xxError_shouldThrowBankUnavailableException() {
        var errorJson = """
                {"error": "internal_error", "message": "Server error"}
                """;

        mockServer.expect(requestTo("/authorizations"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(errorJson)
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() ->
                bankClient.authorize(
                        new AuthorizeBankRequest(5000L, "4111111111111111", "123", 12, 2028),
                        "idem-key"
                )
        )
                .isInstanceOf(BankUnavailableException.class)
                .hasMessageContaining("Server error");
    }

    @Test
    void capture_withSuccessfulResponse_shouldReturnCaptureBankResponse() {
        var responseJson = """
                {
                    "amount": 5000,
                    "authorization_id": "auth-123",
                    "capture_id": "capture-456",
                    "captured_at": "2026-07-16T00:00:00Z",
                    "status": "SUCCESS"
                }
                """;

        mockServer.expect(requestTo("/captures"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        var response = bankClient.capture(
                new CaptureBankRequest(5000L, "auth-123"),
                "idem-key-cap"
        );

        assertThat(response.captureId()).isEqualTo("capture-456");
        mockServer.verify();
    }

    @Test
    void _void_withSuccessfulResponse_shouldReturnVoidBankResponse() {
        var responseJson = """
                {
                    "authorization_id": "auth-123",
                    "status": "SUCCESS",
                    "void_id": "void-789",
                    "voided_at": "2026-07-16T00:00:00Z"
                }
                """;

        mockServer.expect(requestTo("/void"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        var response = bankClient._void(
                new VoidBankRequest("auth-123"),
                "idem-key-void"
        );

        assertThat(response.voidId()).isEqualTo("void-789");
        mockServer.verify();
    }

    @Test
    void refund_withSuccessfulResponse_shouldReturnRefundBankResponse() {
        var responseJson = """
                {
                    "amount": 5000,
                    "capture_id": "capture-456",
                    "currency": "USD",
                    "refund_id": "refund-012",
                    "status": "SUCCESS",
                    "refunded_at": "2026-07-16T00:00:00Z"
                }
                """;

        mockServer.expect(requestTo("/refunds"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        var response = bankClient.refund(
                new RefundBankRequest("capture-456", 5000L),
                "idem-key-ref"
        );

        assertThat(response.refundId()).isEqualTo("refund-012");
        mockServer.verify();
    }

}
