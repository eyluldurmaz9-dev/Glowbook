package glowbook.controller;

import glowbook.entity.Appointment;
import glowbook.entity.AppointmentStatus;
import glowbook.entity.Employee;
import glowbook.repository.AppointmentRepository;
import glowbook.repository.EmployeeRepository;
import glowbook.repository.EmployeeServiceRepository;
import glowbook.repository.ServiceOptionRepository;
import glowbook.security.JwtTokenService;
import glowbook.security.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeCompetencyIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtTokenService jwtTokenService;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired EmployeeServiceRepository employeeServiceRepository;
    @Autowired ServiceOptionRepository serviceOptionRepository;
    @Autowired AppointmentRepository appointmentRepository;

    private String adminToken;
    private String customerToken;
    private Integer serviceId;
    private Integer firstOptionId;
    private Integer secondOptionId;

    @BeforeEach
    void setUp() {
        adminToken = jwtTokenService.generateToken("TESTADMIN", UserRole.ADMIN);
        customerToken = jwtTokenService.generateToken("999999", UserRole.CUSTOMER);
        var optionsByService = serviceOptionRepository.findAll().stream()
                .filter(option -> Boolean.TRUE.equals(option.getActive()))
                .collect(java.util.stream.Collectors.groupingBy(option -> option.getService().getServiceId()));
        var entry = optionsByService.entrySet().stream()
                .filter(item -> item.getValue().size() >= 2)
                .findFirst().orElseThrow();
        serviceId = entry.getKey();
        firstOptionId = entry.getValue().get(0).getOptionId();
        secondOptionId = entry.getValue().get(1).getOptionId();
        appointmentRepository.deleteAll();
        employeeServiceRepository.deleteByEmployeeEmployeeId("COMPTEST");
        employeeRepository.deleteById("COMPTEST");
    }

    @Test
    void adminCreatesEditsAndRemovesOptionCompetencies() throws Exception {
        createEmployee(List.of(firstOptionId));

        mockMvc.perform(get("/api/catalog/services/{serviceId}/options/{optionId}/employees", serviceId, firstOptionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.employeeId == 'COMPTEST')]").exists());
        mockMvc.perform(get("/api/catalog/services/{serviceId}/options/{optionId}/employees", serviceId, secondOptionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.employeeId == 'COMPTEST')]").doesNotExist());

        mockMvc.perform(put("/api/admin/employees/COMPTEST/services")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("optionIds", List.of(secondOptionId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].optionId").value(secondOptionId));

        assertThat(employeeServiceRepository.employeeCanProvideOption("COMPTEST", serviceId, firstOptionId)).isFalse();
        assertThat(employeeServiceRepository.employeeCanProvideOption("COMPTEST", serviceId, secondOptionId)).isTrue();

        mockMvc.perform(put("/api/admin/employees/COMPTEST/services")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("optionIds", List.of(firstOptionId)))))
                .andExpect(status().isForbidden());
    }

    @Test
    void bookingRejectsMismatchedAndInactiveEmployeeWithTurkishMessage() throws Exception {
        createEmployee(List.of(firstOptionId));
        Map<String, Object> mismatched = appointmentPayload(secondOptionId);
        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mismatched)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Seçilen personel bu hizmeti vermiyor."));

        mockMvc.perform(delete("/api/admin/employees/COMPTEST")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(get("/api/catalog/services/{serviceId}/options/{optionId}/employees", serviceId, firstOptionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.employeeId == 'COMPTEST')]").doesNotExist());
        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appointmentPayload(firstOptionId))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deactivationPreservesHistoricalAppointmentsAndAssignments() throws Exception {
        createEmployee(List.of(firstOptionId));
        Employee employee = employeeRepository.findById("COMPTEST").orElseThrow();
        var option = serviceOptionRepository.findById(firstOptionId).orElseThrow();
        Appointment historical = appointmentRepository.save(Appointment.builder()
                .employee(employee)
                .service(option.getService())
                .serviceOption(option)
                .customerName("History")
                .customerSurname("Preserved")
                .phone("05550000123")
                .appointmentDate(LocalDate.now().minusDays(10))
                .appointmentTime(LocalTime.of(10, 0))
                .price(BigDecimal.valueOf(option.getPrice()))
                .status(AppointmentStatus.COMPLETED)
                .build());

        mockMvc.perform(delete("/api/admin/employees/COMPTEST")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        assertThat(appointmentRepository.findById(historical.getAppointmentId())).isPresent();
        assertThat(employeeRepository.findById("COMPTEST").orElseThrow().getActive()).isFalse();
        assertThat(employeeServiceRepository.findByEmployeeEmployeeIdOrderByServiceServiceNameAsc("COMPTEST"))
                .hasSize(1);
    }

    private void createEmployee(List<Integer> optionIds) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("employeeId", "COMPTEST");
        payload.put("firstName", "Competency");
        payload.put("lastName", "Tester");
        payload.put("password", "safe-test-password");
        payload.put("email", "competency@glowbook.test");
        payload.put("active", true);
        payload.put("optionIds", optionIds);
        mockMvc.perform(post("/api/admin/employees")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignedServices.length()").value(optionIds.size()));
    }

    private Map<String, Object> appointmentPayload(Integer optionId) {
        var option = serviceOptionRepository.findById(optionId).orElseThrow();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("employeeId", "COMPTEST");
        payload.put("serviceId", option.getService().getServiceId());
        payload.put("optionId", optionId);
        payload.put("customerName", "Guest");
        payload.put("customerSurname", "User");
        payload.put("phone", "05550000456");
        payload.put("appointmentDate", LocalDate.now().plusDays(2).toString());
        payload.put("appointmentTime", "10:00");
        return payload;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
