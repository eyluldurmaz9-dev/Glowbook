package glowbook.controller;

import tools.jackson.databind.ObjectMapper;
import glowbook.entity.Customer;
import glowbook.repository.AppointmentRepository;
import glowbook.repository.CustomerRepository;
import glowbook.repository.EmployeeRepository;
import glowbook.repository.EmployeeServiceRepository;
import glowbook.repository.NotificationRepository;
import glowbook.repository.ServiceOptionRepository;
import glowbook.security.JwtTokenService;
import glowbook.security.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the customer-facing {@code PATCH /api/appointments/{id}/reschedule} endpoint:
 * ownership authorization, employee reassignment with competency re-check, slot-conflict
 * revalidation that excludes the appointment's own current slot, and that a failed
 * reschedule never mutates the original appointment.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AppointmentRescheduleIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired EmployeeServiceRepository employeeServiceRepository;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired ServiceOptionRepository serviceOptionRepository;
    @Autowired CustomerRepository customerRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtTokenService jwtTokenService;

    private static final AtomicInteger PHONE_SEQUENCE = new AtomicInteger(1);

    private LocalDate workingDate;
    private String employeeId;
    private String secondEmployeeId;
    private Integer serviceId;
    private Integer optionId;

    @BeforeEach
    void setUp() throws Exception {
        notificationRepository.deleteAll();
        appointmentRepository.deleteAll();
        employeeServiceRepository.deleteByEmployeeEmployeeId("RESCHED2");
        employeeRepository.deleteById("RESCHED2");

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

        String adminToken = jwtTokenService.generateToken("TESTADMIN", UserRole.ADMIN);
        Map<String, Object> employeePayload = new LinkedHashMap<>();
        employeePayload.put("employeeId", "RESCHED2");
        employeePayload.put("firstName", "Reschedule");
        employeePayload.put("lastName", "Alternate");
        employeePayload.put("password", "safe-test-password");
        employeePayload.put("email", "reschedule-alt@glowbook.test");
        employeePayload.put("active", true);
        employeePayload.put("optionIds", List.of(optionId));
        mockMvc.perform(post("/api/admin/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employeePayload)))
                .andExpect(status().isOk());
        secondEmployeeId = "RESCHED2";
    }

    @Test
    void ownerCanRescheduleTimeAndEmployee() throws Exception {
        Booked booked = bookAppointment(employeeId, "10:00");

        mockMvc.perform(patch("/api/appointments/{id}/reschedule", booked.appointmentId)
                        .header("Authorization", "Bearer " + booked.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reschedulePayload(secondEmployeeId, "11:00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeId").value(secondEmployeeId))
                .andExpect(jsonPath("$.data.appointmentTime").value("11:00:00"));
    }

    @Test
    void reschedulingToOwnCurrentSlotIsNotTreatedAsAConflict() throws Exception {
        Booked booked = bookAppointment(employeeId, "10:00");

        mockMvc.perform(patch("/api/appointments/{id}/reschedule", booked.appointmentId)
                        .header("Authorization", "Bearer " + booked.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reschedulePayload(employeeId, "10:00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeId").value(employeeId))
                .andExpect(jsonPath("$.data.appointmentTime").value("10:00:00"));
    }

    @Test
    void otherCustomerCannotRescheduleSomeoneElsesAppointment() throws Exception {
        Booked booked = bookAppointment(employeeId, "10:00");
        Booked otherCustomer = bookAppointment(employeeId, "13:00");

        mockMvc.perform(patch("/api/appointments/{id}/reschedule", booked.appointmentId)
                        .header("Authorization", "Bearer " + otherCustomer.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reschedulePayload(employeeId, "11:00"))))
                .andExpect(status().isForbidden());

        var stillOriginal = appointmentRepository.findById(booked.appointmentId).orElseThrow();
        assertThat(stillOriginal.getAppointmentTime().toString()).isEqualTo("10:00");
    }

    @Test
    void rejectsRescheduleOntoAnAlreadyTakenSlotAndKeepsOriginal() throws Exception {
        Booked movable = bookAppointment(employeeId, "10:00");
        bookAppointment(employeeId, "11:00");

        mockMvc.perform(patch("/api/appointments/{id}/reschedule", movable.appointmentId)
                        .header("Authorization", "Bearer " + movable.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reschedulePayload(employeeId, "11:00"))))
                .andExpect(status().isConflict());

        var stillOriginal = appointmentRepository.findById(movable.appointmentId).orElseThrow();
        assertThat(stillOriginal.getAppointmentTime().toString()).isEqualTo("10:00");
    }

    @Test
    void rejectsReassignmentToAnUnqualifiedEmployee() throws Exception {
        Booked booked = bookAppointment(employeeId, "10:00");

        mockMvc.perform(patch("/api/appointments/{id}/reschedule", booked.appointmentId)
                        .header("Authorization", "Bearer " + booked.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reschedulePayload("MISSINGEMP", "11:00"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelledAppointmentCannotBeRescheduled() throws Exception {
        Booked booked = bookAppointment(employeeId, "10:00");
        mockMvc.perform(patch("/api/appointments/{id}/cancel", booked.appointmentId)
                        .header("Authorization", "Bearer " + booked.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/appointments/{id}/reschedule", booked.appointmentId)
                        .header("Authorization", "Bearer " + booked.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reschedulePayload(employeeId, "11:00"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("İptal edilmiş bir randevu değiştirilemez."));
    }

    private record Booked(int appointmentId, int ownerCustomerId, JwtTokenService jwtTokenService) {
        String ownerToken() {
            return jwtTokenService.generateToken(String.valueOf(ownerCustomerId), UserRole.CUSTOMER);
        }
    }

    private Booked bookAppointment(String withEmployeeId, String time) throws Exception {
        Customer customer = customerRepository.save(Customer.builder()
                .firstName("Owner").lastName("Customer")
                .phone(String.format("0555%07d", PHONE_SEQUENCE.getAndIncrement()))
                .email("reschedule-owner-" + System.nanoTime() + "@glowbook.test")
                .password(passwordEncoder.encode("test-password")).active(true).build());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("customerId", customer.getCustomerId());
        payload.put("employeeId", withEmployeeId);
        payload.put("serviceId", serviceId);
        payload.put("optionId", optionId);
        payload.put("appointmentDate", workingDate.toString());
        payload.put("appointmentTime", time);

        String response = mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + jwtTokenService.generateToken(
                                customer.getCustomerId().toString(), UserRole.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map<?, ?> parsed = objectMapper.readValue(response, Map.class);
        Map<?, ?> data = (Map<?, ?>) parsed.get("data");
        int appointmentId = (Integer) data.get("appointmentId");
        return new Booked(appointmentId, customer.getCustomerId(), jwtTokenService);
    }

    private Map<String, Object> reschedulePayload(String withEmployeeId, String time) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("employeeId", withEmployeeId);
        payload.put("appointmentDate", workingDate.toString());
        payload.put("appointmentTime", time);
        return payload;
    }
}
