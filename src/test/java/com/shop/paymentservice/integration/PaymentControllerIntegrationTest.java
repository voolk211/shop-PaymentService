package com.shop.paymentservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;

import com.shop.paymentservice.model.dto.PaymentCreateDto;
import com.shop.paymentservice.model.entities.Payment;
import com.shop.paymentservice.model.entities.PaymentStatus;
import com.shop.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {
        "external.random-number-api.url=http://localhost:8080",
        "internal.internal-secret=test-secret",
        "spring.kafka.topics.payment-events=payment-events"
})
public class PaymentControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Value("${internal.internal-secret}")
    private String internalSecret;

    @BeforeEach
    void cleanDatabase() {
        paymentRepository.deleteAll();
    }

    @BeforeEach
    void setupWiremock() {
        WireMock.configureFor(
                wiremock.getHost(),
                wiremock.getMappedPort(8080)
        );
        WireMock.reset();
    }

    private RequestPostProcessor withUserHeaders(Long userId, String... roles) {
        return request -> {
            request.addHeader("X-User-Id", userId.toString());
            request.addHeader("X-Roles", "ROLE_" + String.join(",ROLE_", roles));
            request.addHeader("X-Internal-Auth", internalSecret);
            return request;
        };
    }

    private void stubRandomNumberApi(Long number) {
        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/random"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[%d]".formatted(number))));
    }

    private PaymentCreateDto validPaymentCreateDto(Long orderId, Long userId) {
        PaymentCreateDto dto = new PaymentCreateDto();
        dto.setOrderId(orderId);
        dto.setUserId(userId);
        dto.setTimestamp(LocalDateTime.now());
        dto.setPaymentAmount(new BigDecimal("100.00"));
        return dto;
    }

    @Test
    void createPayment_WhenEvenNumber_ShouldReturnCreatedWithSuccessStatus() throws Exception {
        stubRandomNumberApi(2L);

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .with(withUserHeaders(1L, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentCreateDto(1L, 1L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void createPayment_WhenOddNumber_ShouldReturnCreatedWithFailedStatus() throws Exception {
        stubRandomNumberApi(3L);

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .with(withUserHeaders(1L, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentCreateDto(1L, 1L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void createPayment_WhenExternalApiDown_ShouldReturn502() throws Exception {
        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/random"))
                .willReturn(WireMock.aResponse()
                        .withStatus(503)));

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .with(withUserHeaders(1L, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentCreateDto(1L, 1L))))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void createPayment_WhenMissingAuthHeaders_ShouldReturn401() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentCreateDto(1L, 1L))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPayment_WhenInvalidDto_ShouldReturn400() throws Exception {
        PaymentCreateDto invalidDto = new PaymentCreateDto();

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .with(withUserHeaders(1L, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    void createPayment_ShouldPersistToMongoDB() throws Exception {
        stubRandomNumberApi(2L);

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .with(withUserHeaders(1L, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentCreateDto(1L, 1L))))
                .andExpect(status().isCreated());

        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments).hasSize(1);
        assertThat(payments.getFirst().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payments.getFirst().getOrderId()).isEqualTo(1L);
    }

    @Test
    void getPayments_WhenNoFilters_ShouldReturnAllPayments() throws Exception {
        stubRandomNumberApi(2L);
        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .with(withUserHeaders(1L, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentCreateDto(1L, 1L))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/payments")
                        .with(withUserHeaders(1L, "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getPayments_WhenFilterByUserId_ShouldReturnOnlyUserPayments() throws Exception {
        stubRandomNumberApi(2L);
        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .with(withUserHeaders(1L, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentCreateDto(1L, 1L))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .with(withUserHeaders(2L, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentCreateDto(2L, 2L))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/payments")
                        .with(withUserHeaders(1L, "USER"))
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(1));
    }

    @Test
    void getPayments_WhenFilterByStatus_ShouldReturnOnlyMatchingPayments() throws Exception {
        stubRandomNumberApi(2L);
        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .with(withUserHeaders(1L, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentCreateDto(1L, 1L))))
                .andExpect(status().isCreated());

        stubRandomNumberApi(3L);
        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .with(withUserHeaders(1L, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentCreateDto(2L, 1L))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/payments")
                        .with(withUserHeaders(1L, "USER"))
                        .param("paymentStatus", "SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("SUCCESS"));
    }

    @Test
    void getPayments_WhenMissingAuth_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getTotalSumForUser_WhenPaymentsExist_ShouldReturnCorrectSum() throws Exception {
        stubRandomNumberApi(2L);
        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .with(withUserHeaders(1L, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentCreateDto(1L, 1L))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .with(withUserHeaders(1L, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentCreateDto(2L, 1L))))
                .andExpect(status().isCreated());

        org.bson.Document raw = mongoTemplate.getCollection("payments")
                .find()
                .first();
        System.out.println("Raw MongoDB document: " + raw.toJson());

        mockMvc.perform(get("/api/payments/total/1")
                        .with(withUserHeaders(1L, "USER")))
                .andExpect(status().isOk())
                .andExpect(content().string("200.00"));
    }

    @Test
    void getTotalSumForUser_WhenNoPayments_ShouldReturnZero() throws Exception {
        mockMvc.perform(get("/api/payments/total/999")
                        .with(withUserHeaders(1L, "USER")))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    @Test
    void getTotalSumForUser_WhenDateRangeProvided_ShouldReturnCorrectSum() throws Exception {
        stubRandomNumberApi(2L);
        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .with(withUserHeaders(1L, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentCreateDto(1L, 1L))))
                .andExpect(status().isCreated());

        List<Payment> all = paymentRepository.findAll();
        System.out.println("Payments in DB: " + all);
        System.out.println("Count: " + all.size());

        mockMvc.perform(get("/api/payments/total/1")
                        .with(withUserHeaders(1L, "USER"))
                        .param("from", LocalDateTime.now().minusDays(1)
                                .format(DateTimeFormatter.ISO_DATE_TIME))
                        .param("to", LocalDateTime.now().plusDays(1)
                                .format(DateTimeFormatter.ISO_DATE_TIME)))
                .andExpect(status().isOk())
                .andExpect(content().string("100.00"));
    }

    @Test
    void getTotalSumForAllUsers_WhenPaymentsExist_ShouldReturnCorrectSum() throws Exception {
        stubRandomNumberApi(2L);
        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .with(withUserHeaders(1L, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentCreateDto(1L, 1L))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .with(withUserHeaders(2L, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentCreateDto(2L, 2L))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/payments/total")
                        .with(withUserHeaders(1L, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string("200.00"));
    }

    @Test
    void getTotalSumForAllUsers_WhenNoPayments_ShouldReturnZero() throws Exception {
        mockMvc.perform(get("/api/payments/total")
                        .with(withUserHeaders(1L, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }
}
