package com.osigie.payment_gateway.controller;

import com.osigie.payment_gateway.AbstractIntegrationTest;
import com.osigie.payment_gateway.domain.entity.Merchant;
import com.osigie.payment_gateway.domain.entity.Merchant;
import com.osigie.payment_gateway.dto.payment.CardDetailsDto;
import com.osigie.payment_gateway.dto.payment.CreateAuthorizationRequestDto;
import com.osigie.payment_gateway.repository.IdempotencyKeyRepository;
import com.osigie.payment_gateway.repository.MerchantRepository;
import com.osigie.payment_gateway.repository.PaymentRepository;
import com.osigie.payment_gateway.repository.TransactionRepository;
import com.osigie.payment_gateway.service.BankClient;
import com.osigie.payment_gateway.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentControllerE2ETests extends AbstractIntegrationTest {

    private static final String PAYMENTS_URL = "/api/v1/payments";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BankClient bankClient;

    private String testApiKey;

    @Configuration
    static class TestMockConfig {
        @Bean
        @Primary
        BankClient bankClient() {
            return mock(BankClient.class);
        }
    }

    @BeforeEach
    void setUp() {
        idempotencyKeyRepository.deleteAllInBatch();
        transactionRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        merchantRepository.deleteAllInBatch();

        Merchant testMerchant = merchantRepository.save(new Merchant("Test Merchant", "test-api-key-" + UUID.randomUUID()));
        testApiKey = testMerchant.getApiKey();
    }

    @Nested
    @DisplayName("Create Authorization")
    class CreateAuthorizationTests {

        @Test
        void givenValidRequest_whenCreateAuthorize_thenReturn200() throws Exception {
            var request = TestDataFactory.createTestAuthorizationRequest();

            when(bankClient.authorize(any(), any()))
                    .thenReturn(TestDataFactory.createAuthorizeBankResponse());

            mockMvc.perform(post(PAYMENTS_URL)
                            .header("x-api-key", testApiKey)
                            .header("x-idempotency-key", "e2e-auth-1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("AUTHORIZED"));
        }

        @Test
        void givenMissingApiKey_whenCreateAuthorize_thenReturn401() throws Exception {
            var request = TestDataFactory.createTestAuthorizationRequest();

            mockMvc.perform(post(PAYMENTS_URL)
                            .header("x-idempotency-key", "e2e-auth-2")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenMissingIdempotencyKey_whenCreateAuthorize_thenReturn400() throws Exception {
            var request = TestDataFactory.createTestAuthorizationRequest();

            mockMvc.perform(post(PAYMENTS_URL)
                            .header("x-api-key", testApiKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenBlankMerchantOrderId_whenCreateAuthorize_thenReturn400() throws Exception {
            var invalid = new CreateAuthorizationRequestDto(
                    " ", "cust-456", 5000L, TestDataFactory.createTestCardDetails());

            mockMvc.perform(post(PAYMENTS_URL)
                            .header("x-api-key", testApiKey)
                            .header("x-idempotency-key", "e2e-auth-3")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenBlankMerchantCustomerId_whenCreateAuthorize_thenReturn400() throws Exception {
            var invalid = new CreateAuthorizationRequestDto(
                    "order-123", "   ", 5000L, TestDataFactory.createTestCardDetails());

            mockMvc.perform(post(PAYMENTS_URL)
                            .header("x-api-key", testApiKey)
                            .header("x-idempotency-key", "e2e-auth-4")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenZeroAmount_whenCreateAuthorize_thenReturn400() throws Exception {
            var invalid = new CreateAuthorizationRequestDto(
                    "order-123", "cust-456", 5L, TestDataFactory.createTestCardDetails());

            mockMvc.perform(post(PAYMENTS_URL)
                            .header("x-api-key", testApiKey)
                            .header("x-idempotency-key", "e2e-auth-5")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenInvalidExpiryMonth_whenCreateAuthorize_thenReturn400() throws Exception {
            var invalidCard = new CardDetailsDto("4111111111111111", "123", 13, 2028);
            var invalid = new CreateAuthorizationRequestDto(
                    "order-123", "cust-456", 5000L, invalidCard);

            mockMvc.perform(post(PAYMENTS_URL)
                            .header("x-api-key", testApiKey)
                            .header("x-idempotency-key", "e2e-auth-6")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenMissingCardDetails_whenCreateAuthorize_thenReturn400() throws Exception {
            var json = """
                    {
                        "merchantOrderId": "order-123",
                        "merchantCustomerId": "cust-456",
                        "amountMinor": 5000,
                        "cardDetails": null
                    }
                    """;

            mockMvc.perform(post(PAYMENTS_URL)
                            .header("x-api-key", testApiKey)
                            .header("x-idempotency-key", "e2e-auth-7")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Get Payments")
    class GetPaymentsTests {

        @Test
        void givenDefaultPagination_whenGetPayments_thenReturn200() throws Exception {
            mockMvc.perform(get(PAYMENTS_URL)
                            .header("x-api-key", testApiKey))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalRecords").value(0))
                    .andExpect(jsonPath("$.pageNo").value(0))
                    .andExpect(jsonPath("$.pageSize").value(10));
        }

        @Test
        void givenNegativePage_whenGetPayments_thenReturn400() throws Exception {
            mockMvc.perform(get(PAYMENTS_URL)
                            .header("x-api-key", testApiKey)
                            .param("page", "-1"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenExcessivePageSize_whenGetPayments_thenReturn400() throws Exception {
            mockMvc.perform(get(PAYMENTS_URL)
                            .header("x-api-key", testApiKey)
                            .param("size", "200"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Get Payment")
    class GetPaymentTests {

        @Test
        void givenExistingPayment_whenGetPayment_thenReturn200() throws Exception {
            var request = TestDataFactory.createTestAuthorizationRequest();

            when(bankClient.authorize(any(), any()))
                    .thenReturn(TestDataFactory.createAuthorizeBankResponse());

            var createResult = mockMvc.perform(post(PAYMENTS_URL)
                            .header("x-api-key", testApiKey)
                            .header("x-idempotency-key", "e2e-get-1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = createResult.getResponse().getContentAsString();
            var paymentId = objectMapper.readTree(responseBody).get("data").get("id").asText();

            mockMvc.perform(get(PAYMENTS_URL + "/{paymentId}", paymentId)
                            .header("x-api-key", testApiKey))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(paymentId));
        }

        @Test
        void givenNonExistentPayment_whenGetPayment_thenReturn404() throws Exception {
            mockMvc.perform(get(PAYMENTS_URL + "/{paymentId}", "00000000-0000-0000-0000-000000000000")
                            .header("x-api-key", testApiKey))
                    .andExpect(status().isNotFound());
        }
    }

}
