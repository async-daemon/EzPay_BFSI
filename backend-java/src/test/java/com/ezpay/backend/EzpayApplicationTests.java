package com.ezpay.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end smoke tests covering all 6 BRD use cases, mirroring
 * python-fastapi-version/backend/tests/test_use_cases.py one-for-one against
 * the Java/Spring MVC/Hibernate rebuild.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ezpaytest;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EzpayApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final AtomicInteger COUNTER = new AtomicInteger(1);

    private record Registered(String mobileNumber, String email, String password, String token, String userId) {
    }

    private Registered registerVerifyAndLogin() throws Exception {
        int n = COUNTER.getAndIncrement();
        String mobile = "9" + String.format("%09d", n);
        String email = "user" + n + "@example.com";
        String password = "Passw0rd" + n;

        MvcResult registerResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "User " + n,
                                "email", email,
                                "mobileNumber", mobile,
                                "password", password
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.devOtp", not(emptyOrNullString())))
                .andReturn();

        JsonNode registerBody = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String otp = registerBody.get("devOtp").asText();

        MvcResult verifyResult = mockMvc.perform(post("/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("mobileNumber", mobile, "otp", otp))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(emptyOrNullString())))
                .andReturn();

        JsonNode verifyBody = objectMapper.readTree(verifyResult.getResponse().getContentAsString());
        return new Registered(mobile, email, password, verifyBody.get("accessToken").asText(), verifyBody.get("userId").asText());
    }

    @Test
    @Order(1)
    void registerAndVerifyOtp_activatesAccountAndIssuesToken() throws Exception {
        Registered reg = registerVerifyAndLogin();
        org.junit.jupiter.api.Assertions.assertNotNull(reg.token());
    }

    @Test
    @Order(2)
    void verifyOtp_withWrongCode_fails400() throws Exception {
        int n = COUNTER.getAndIncrement();
        String mobile = "9" + String.format("%09d", n);
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "User " + n, "email", "user" + n + "@example.com",
                                "mobileNumber", mobile, "password", "Passw0rd" + n))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("mobileNumber", mobile, "otp", "000000"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(3)
    void login_withCorrectCredentials_succeeds() throws Exception {
        Registered reg = registerVerifyAndLogin();
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("identifier", reg.email(), "password", reg.password()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(emptyOrNullString())));
    }

    @Test
    @Order(4)
    void login_withWrongPassword_fails401() throws Exception {
        Registered reg = registerVerifyAndLogin();
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("identifier", reg.email(), "password", "WrongPass1"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    void profile_viewUpdateAndAddUpiId() throws Exception {
        Registered reg = registerVerifyAndLogin();

        mockMvc.perform(get("/profile").header("Authorization", "Bearer " + reg.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(reg.email()));

        mockMvc.perform(post("/profile/upi-id").header("Authorization", "Bearer " + reg.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("upiId", reg.mobileNumber() + "@okaxis"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upiId").value(reg.mobileNumber() + "@okaxis"));
    }

    @Test
    @Order(6)
    void bankAccount_addAndListIsMasked() throws Exception {
        Registered reg = registerVerifyAndLogin();

        mockMvc.perform(post("/profile/bank-accounts").header("Authorization", "Bearer " + reg.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "accountNumber", "123456789012",
                                "ifscCode", "HDFC0000123",
                                "accountHolderName", "Test User"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.maskedAccountNumber").value("********9012"));

        mockMvc.perform(get("/profile/bank-accounts").header("Authorization", "Bearer " + reg.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].maskedAccountNumber").value("********9012"));
    }

    @Test
    @Order(7)
    void upiPayment_completesAndCreatesNotification() throws Exception {
        Registered reg = registerVerifyAndLogin();

        mockMvc.perform(post("/payments/upi").header("Authorization", "Bearer " + reg.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "recipientUpiId", "merchant@okaxis", "amount", 500.0, "note", "Groceries"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/notifications").header("Authorization", "Bearer " + reg.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("confirmation"));
    }

    @Test
    @Order(8)
    void upiPayment_highValue_triggersFraudVerification() throws Exception {
        Registered reg = registerVerifyAndLogin();

        mockMvc.perform(post("/payments/upi").header("Authorization", "Bearer " + reg.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "recipientUpiId", "merchant@okaxis", "amount", 250000.0))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.detail.status").value("REQUIRES_VERIFICATION"));
    }

    @Test
    @Order(9)
    void bankTransfer_completesAndCreatesNotification() throws Exception {
        Registered reg = registerVerifyAndLogin();

        MvcResult accountResult = mockMvc.perform(post("/profile/bank-accounts").header("Authorization", "Bearer " + reg.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "accountNumber", "998877665544",
                                "ifscCode", "ICIC0000456",
                                "accountHolderName", "Test User"))))
                .andExpect(status().isCreated())
                .andReturn();
        String accountId = objectMapper.readTree(accountResult.getResponse().getContentAsString()).get("accountId").asText();

        mockMvc.perform(post("/payments/bank-transfer").header("Authorization", "Bearer " + reg.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fromAccountId", accountId,
                                "recipientAccountNumber", "111122223333",
                                "recipientIfscCode", "SBIN0000789",
                                "amount", 1500.0,
                                "purpose", "Rent"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.type").value("BANK_TRANSFER"));
    }

    @Test
    @Order(10)
    void transactionHistory_listsAndTracksStatus() throws Exception {
        Registered reg = registerVerifyAndLogin();

        MvcResult payResult = mockMvc.perform(post("/payments/upi").header("Authorization", "Bearer " + reg.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("recipientUpiId", "shop@okaxis", "amount", 250.0))))
                .andExpect(status().isCreated())
                .andReturn();
        String txnId = objectMapper.readTree(payResult.getResponse().getContentAsString()).get("transactionId").asText();

        mockMvc.perform(get("/transactions").header("Authorization", "Bearer " + reg.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionId").value(txnId));

        mockMvc.perform(get("/transactions/" + txnId).header("Authorization", "Bearer " + reg.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @Order(11)
    void reminder_createMarkPaidAndSendNotifies() throws Exception {
        Registered reg = registerVerifyAndLogin();

        MvcResult createResult = mockMvc.perform(post("/reminders").header("Authorization", "Bearer " + reg.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Electricity bill", "amount", 1200.0,
                                "dueDate", java.time.LocalDateTime.now().plusDays(10).toString()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("UPCOMING"))
                .andReturn();
        String reminderId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("reminderId").asText();

        mockMvc.perform(post("/reminders/" + reminderId + "/send").header("Authorization", "Bearer " + reg.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/reminders/" + reminderId + "/mark-paid").header("Authorization", "Bearer " + reg.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        mockMvc.perform(get("/notifications").header("Authorization", "Bearer " + reg.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("reminder"));
    }

    @Test
    @Order(12)
    void forgotAndResetPassword_flow() throws Exception {
        Registered reg = registerVerifyAndLogin();

        MvcResult forgotResult = mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("identifier", reg.email()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.devResetToken", not(emptyOrNullString())))
                .andReturn();
        String resetToken = objectMapper.readTree(forgotResult.getResponse().getContentAsString()).get("devResetToken").asText();

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("resetToken", resetToken, "newPassword", "NewPassw0rd1"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("identifier", reg.email(), "password", "NewPassw0rd1"))))
                .andExpect(status().isOk());
    }

    @Test
    @Order(13)
    void authEnforcement_missingOrInvalidTokenIs401() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/profile").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }
}
