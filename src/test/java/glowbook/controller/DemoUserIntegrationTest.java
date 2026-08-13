package glowbook.controller;

import glowbook.repository.EmployeeServiceRepository;
import glowbook.security.JwtTokenService;
import glowbook.security.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.demo-users.enabled=true",
        "app.demo-users.admin-password=test-admin-password",
        "app.demo-users.employee-password=test-employee-password",
        "app.demo-users.customer-password=test-customer-password"
})
@AutoConfigureMockMvc
class DemoUserIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired EmployeeServiceRepository employeeServiceRepository;
    @Autowired JwtTokenService jwtTokenService;

    @Test
    void demoAccountsAuthenticateWithExpectedRolesAndAuthorization() throws Exception {
        AuthResult admin = login("admin@glowbook.test", "test-admin-password", "ADMIN", "ADMIN");
        AuthResult employee = login("employee@glowbook.test", "test-employee-password", "EMPLOYEE", "EMPLOYEE");
        AuthResult customer = login("customer@glowbook.test", "test-customer-password", "CUSTOMER", "CUSTOMER");

        assertThat(jwtTokenService.getRole(admin.token())).isEqualTo(UserRole.ADMIN);
        assertThat(jwtTokenService.getRole(employee.token())).isEqualTo(UserRole.EMPLOYEE);
        assertThat(jwtTokenService.getRole(customer.token())).isEqualTo(UserRole.CUSTOMER);
        assertThat(employee.employeeId()).isEqualTo("DEMOEMP");

        mockMvc.perform(get("/api/appointments/employee/DEMOEMP")
                        .param("startDate", LocalDate.now().toString())
                        .param("endDate", LocalDate.now().plusDays(7).toString())
                        .header("Authorization", bearer(employee.token())))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/appointments/employee/OTHEREMP")
                        .param("startDate", LocalDate.now().toString())
                        .param("endDate", LocalDate.now().plusDays(7).toString())
                        .header("Authorization", bearer(employee.token())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/appointments/employee/DEMOEMP")
                        .param("startDate", LocalDate.now().toString())
                        .param("endDate", LocalDate.now().plusDays(7).toString())
                        .header("Authorization", bearer(customer.token())))
                .andExpect(status().isForbidden());

        String employeePayload = objectMapper.writeValueAsString(Map.of(
                "employeeId", "AUTHZTEST",
                "firstName", "Auth",
                "lastName", "Test",
                "password", "not-used-password",
                "active", true,
                "optionIds", java.util.List.of()));
        mockMvc.perform(post("/api/admin/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeePayload)
                        .header("Authorization", bearer(employee.token())))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeePayload)
                        .header("Authorization", bearer(customer.token())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/customers").header("Authorization", bearer(admin.token())))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/customers").header("Authorization", bearer(employee.token())))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/customers").header("Authorization", bearer(customer.token())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/customers"))
                .andExpect(status().isUnauthorized());

        var demoAssignments = employeeServiceRepository.findAll().stream()
                .filter(item -> "DEMOEMP".equals(item.getEmployee().getEmployeeId()))
                .toList();
        assertThat(demoAssignments).isNotEmpty().hasSizeLessThanOrEqualTo(3);
        assertThat(demoAssignments).allMatch(item -> item.getServiceOption() != null);
    }

    @Test
    void invalidPasswordAndWrongRoleAreRejectedWithSafeMessages() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload("admin@glowbook.test", "wrong-password", "ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Kullanıcı adı veya şifre hatalı."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload("employee@glowbook.test", "test-employee-password", "ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bu hesap yönetici hesabı değil."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload("admin@glowbook.test", "test-admin-password", "EMPLOYEE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bu hesap personel hesabı değil."));
    }

    @Test
    void publicRegistrationCannotEscalateRole() throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Public",
                                "lastName", "Customer",
                                "phone", "05559998877",
                                "password", "safe-password",
                                "email", "public-role-test@glowbook.test",
                                "role", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(response).path("data").path("token").asText();
        assertThat(jwtTokenService.getRole(token)).isEqualTo(UserRole.CUSTOMER);
        mockMvc.perform(get("/api/admin/customers").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    private AuthResult login(String username, String password, String requestedRole, String expectedRole) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload(username, password, requestedRole)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value(expectedRole))
                .andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(response).path("data");
        return new AuthResult(data.path("token").asText(), data.path("employeeId").asText(null));
    }

    private String loginPayload(String username, String password, String role) throws Exception {
        return objectMapper.writeValueAsString(Map.of("username", username, "password", password, "role", role));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record AuthResult(String token, String employeeId) {
    }
}
