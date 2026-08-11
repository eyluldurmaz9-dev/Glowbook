package glowbook.controller;

import tools.jackson.databind.ObjectMapper;
import glowbook.repository.EmployeeServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

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

    @Test
    void demoAccountsAuthenticateWithExpectedRolesAndAuthorization() throws Exception {
        String adminToken = login("admin@glowbook.test", "test-admin-password", "ADMIN", "ADMIN");
        String employeeToken = login("employee@glowbook.test", "test-employee-password", "EMPLOYEE", "EMPLOYEE");
        String customerToken = login("customer@glowbook.test", "test-customer-password", "CUSTOMER", "CUSTOMER");

        mockMvc.perform(get("/api/admin/customers").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/customers").header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/customers").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        long assignments = employeeServiceRepository.findAll().stream()
                .filter(item -> "DEMOEMP".equals(item.getEmployee().getEmployeeId()))
                .count();
        org.assertj.core.api.Assertions.assertThat(assignments).isGreaterThan(0);
    }

    private String login(String username, String password, String requestedRole, String expectedRole) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username, "password", password, "role", requestedRole))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value(expectedRole))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("token").asText();
    }
}
