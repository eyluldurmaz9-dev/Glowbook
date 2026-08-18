package glowbook.controller;

import tools.jackson.databind.ObjectMapper;
import glowbook.entity.Customer;
import glowbook.entity.PasswordResetToken;
import glowbook.repository.CustomerRepository;
import glowbook.repository.EmployeeRepository;
import glowbook.repository.PasswordResetTokenRepository;
import glowbook.security.UserRole;
import glowbook.support.FakeMailSender;
import glowbook.support.MailTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the email-based password reset flow end to end: no user
 * enumeration, single-use time-limited tokens, and that a reset never
 * changes which role/account the credentials belong to.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MailTestConfig.class)
class PasswordResetIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CustomerRepository customerRepository;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired PasswordResetTokenRepository tokenRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired FakeMailSender fakeMailSender;

    private static final AtomicInteger SEQUENCE = new AtomicInteger(1);

    private Customer customer;

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();
        employeeRepository.deleteById("RESETEMP");
        fakeMailSender.reset();
        int n = SEQUENCE.getAndIncrement();
        customer = customerRepository.save(Customer.builder()
                .firstName("Reset").lastName("Test")
                .phone(String.format("0555%07d", n))
                .email("reset-flow-" + n + "@glowbook.test")
                .password(passwordEncoder.encode("original-password"))
                .active(true).build());
    }

    @Test
    void forgotPasswordWithRegisteredEmailSendsResetLinkAndGenericMessage() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", customer.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(
                        "Eğer bu e-posta adresi sistemde kayıtlıysa şifre sıfırlama bağlantısı gönderildi."));

        assertThat(fakeMailSender.sendCount()).isEqualTo(1);
        assertThat(fakeMailSender.sends().get(0).to()).isEqualTo(customer.getEmail());
    }

    @Test
    void forgotPasswordWithUnknownEmailReturnsIdenticalResponseAndSendsNothing() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "nobody-registered@glowbook.test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(
                        "Eğer bu e-posta adresi sistemde kayıtlıysa şifre sıfırlama bağlantısı gönderildi."));

        assertThat(fakeMailSender.sendCount()).isZero();
    }

    @Test
    void resetPasswordWithValidTokenChangesPasswordAndOldOneStopsWorking() throws Exception {
        requestReset(customer.getEmail());
        String token = fakeMailSender.lastToken();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("token", token, "newPassword", "brand-new-password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        loginCustomer(customer.getPhone(), "original-password").andExpect(status().isBadRequest());
        loginCustomer(customer.getPhone(), "brand-new-password")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerId").value(customer.getCustomerId()));
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        requestReset(customer.getEmail());
        String token = fakeMailSender.lastToken();

        PasswordResetToken saved = tokenRepository.findAll().get(0);
        saved.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        tokenRepository.save(saved);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("token", token, "newPassword", "does-not-matter-1"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Geçersiz veya süresi dolmuş bağlantı."));

        // Original password must still work — a rejected reset never mutates the account.
        loginCustomer(customer.getPhone(), "original-password").andExpect(status().isOk());
    }

    @Test
    void reusedTokenIsRejectedOnSecondAttempt() throws Exception {
        requestReset(customer.getEmail());
        String token = fakeMailSender.lastToken();
        Map<String, Object> resetPayload =
                Map.of("token", token, "newPassword", "first-reset-password");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetPayload)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("token", token, "newPassword", "second-attempt-password"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Geçersiz veya süresi dolmuş bağlantı."));

        // The first (successful) reset's password is still the active one.
        loginCustomer(customer.getPhone(), "first-reset-password").andExpect(status().isOk());
        loginCustomer(customer.getPhone(), "second-attempt-password").andExpect(status().isBadRequest());
    }

    @Test
    void employeeAccountCanResetTheSameWayWithoutRoleChanging() throws Exception {
        // A dedicated employee, not the shared GLW001 seed row other test
        // classes also use — resetting its password here must never leak
        // into any other test's fixtures.
        employeeRepository.save(glowbook.entity.Employee.builder()
                .employeeId("RESETEMP")
                .firstName("Reset").lastName("Employee")
                .password(passwordEncoder.encode("original-employee-password"))
                .email("reset-employee@glowbook.test")
                .active(true)
                .role(UserRole.EMPLOYEE)
                .build());

        requestReset("reset-employee@glowbook.test");
        String token = fakeMailSender.lastToken();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("token", token, "newPassword", "employee-new-password"))))
                .andExpect(status().isOk());

        Map<String, Object> loginPayload = new LinkedHashMap<>();
        loginPayload.put("username", "RESETEMP");
        loginPayload.put("password", "employee-new-password");
        loginPayload.put("role", "EMPLOYEE");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.data.employeeId").value("RESETEMP"));

        // Original password no longer works.
        Map<String, Object> oldLoginPayload = new LinkedHashMap<>();
        oldLoginPayload.put("username", "RESETEMP");
        oldLoginPayload.put("password", "original-employee-password");
        oldLoginPayload.put("role", "EMPLOYEE");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldLoginPayload)))
                .andExpect(status().isBadRequest());
    }

    private void requestReset(String email) throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email))))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions loginCustomer(String phone, String password)
            throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", phone);
        payload.put("password", password);
        payload.put("role", "CUSTOMER");
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)));
    }
}
