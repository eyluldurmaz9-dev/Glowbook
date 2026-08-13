package glowbook.controller;

import tools.jackson.databind.ObjectMapper;
import glowbook.entity.Customer;
import glowbook.repository.AppointmentRepository;
import glowbook.repository.CustomerRepository;
import glowbook.repository.EmployeeServiceRepository;
import glowbook.repository.NotificationRepository;
import glowbook.repository.ServiceOptionRepository;
import glowbook.security.JwtTokenService;
import glowbook.security.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AppointmentControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired EmployeeServiceRepository employeeServiceRepository;
    @Autowired ServiceOptionRepository serviceOptionRepository;
    @Autowired CustomerRepository customerRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtTokenService jwtTokenService;

    private LocalDate workingDate;
    private String employeeId;
    private Integer serviceId;
    private Integer optionId;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        appointmentRepository.deleteAll();
        workingDate = LocalDate.now().plusDays(1);
        while (workingDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            workingDate = workingDate.plusDays(1);
        }
        var assignment = employeeServiceRepository.findAll().stream()
                .filter(item -> "GLW001".equals(item.getEmployee().getEmployeeId()))
                .findFirst().orElseThrow();
        employeeId = assignment.getEmployee().getEmployeeId();
        serviceId = assignment.getService().getServiceId();
        optionId = serviceOptionRepository.findByServiceServiceIdAndActiveTrueOrderByOptionNameAsc(serviceId)
                .getFirst().getOptionId();
    }

    @Test
    void createsValidGuestAppointment() throws Exception {
        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(guestPayload())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.customerId").doesNotExist())
                .andExpect(jsonPath("$.data.customerName").value("Guest"))
                .andExpect(jsonPath("$.data.phone").value("05551234567"))
                .andExpect(jsonPath("$.data.price").isNumber());
    }

    @Test
    void rejectsGuestWithoutPhone() throws Exception {
        Map<String, Object> payload = guestPayload();
        payload.remove("phone");
        mockMvc.perform(post("/api/appointments").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void returnsNotFoundForInvalidEmployee() throws Exception {
        Map<String, Object> payload = guestPayload();
        payload.put("employeeId", "MISSING");
        mockMvc.perform(post("/api/appointments").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsConflictForUnavailableSlot() throws Exception {
        Map<String, Object> payload = guestPayload();
        payload.put("appointmentTime", "09:00");
        mockMvc.perform(post("/api/appointments").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict());
    }

    @Test
    void availabilityContainsOnlyFullHoursAndEmployeeIdentity() throws Exception {
        mockMvc.perform(get("/api/appointments/available-slots")
                        .param("serviceId", serviceId.toString())
                        .param("optionId", optionId.toString())
                        .param("date", workingDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].employeeId").isNotEmpty())
                .andExpect(jsonPath("$.data[0].employeeName").isNotEmpty())
                .andExpect(jsonPath("$.data[*].availableTimes[?(@ =~ /.*:30(:00)?/)]").doesNotExist());
    }

    @Test
    void rejectsHalfHourStarts() throws Exception {
        for (String time : java.util.List.of("10:30", "11:30")) {
            Map<String, Object> payload = guestPayload();
            payload.put("appointmentTime", time);
            mockMvc.perform(post("/api/appointments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Randevu saati tam saat olmalıdır (örnek: 10:00)."));
        }
    }

    @Test
    void returnsConflictForDuplicateSlot() throws Exception {
        String payload = objectMapper.writeValueAsString(guestPayload());
        mockMvc.perform(post("/api/appointments").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/appointments").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    void createsRegisteredCustomerAppointmentWithMatchingJwt() throws Exception {
        Customer customer = customerRepository.save(Customer.builder()
                .firstName("Registered").lastName("Customer").phone("05551234568")
                .email("registered-booking@glowbook.test")
                .password(passwordEncoder.encode("test-password")).active(true).build());
        Map<String, Object> payload = guestPayload();
        payload.put("customerId", customer.getCustomerId());
        payload.remove("customerName");
        payload.remove("customerSurname");
        payload.remove("phone");

        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + jwtTokenService.generateToken(
                                customer.getCustomerId().toString(), UserRole.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerId").value(customer.getCustomerId()))
                .andExpect(jsonPath("$.data.customerName").value("Registered"));
    }

    private Map<String, Object> guestPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("employeeId", employeeId);
        payload.put("serviceId", serviceId);
        payload.put("optionId", optionId);
        payload.put("customerName", "Guest");
        payload.put("customerSurname", "User");
        payload.put("phone", "05551234567");
        payload.put("appointmentDate", workingDate.toString());
        payload.put("appointmentTime", "10:00");
        return payload;
    }
}
