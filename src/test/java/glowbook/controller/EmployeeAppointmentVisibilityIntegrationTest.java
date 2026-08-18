package glowbook.controller;

import glowbook.repository.AppointmentRepository;
import glowbook.repository.EmployeeServiceRepository;
import glowbook.repository.NotificationRepository;
import glowbook.repository.ServiceOptionRepository;
import glowbook.security.JwtTokenService;
import glowbook.security.UserRole;
import glowbook.support.MutableClock;
import glowbook.support.MutableClockTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests K, L and M: one appointment record is shared between customer and employee views,
 * and backend authorization — not client-side filtering — keeps employees apart.
 */
@SpringBootTest(properties = {
        "app.demo-users.enabled=true",
        "app.demo-users.admin-password=test-admin-password",
        "app.demo-users.employee-password=test-employee-password",
        "app.demo-users.customer-password=test-customer-password"
})
@AutoConfigureMockMvc
@Import(MutableClockTestConfig.class)
class EmployeeAppointmentVisibilityIntegrationTest {

    private static final LocalDateTime MORNING = LocalDateTime.of(2026, 8, 14, 9, 0);
    private static final LocalDate BOOKING_DATE = LocalDate.of(2026, 8, 14);

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MutableClock clock;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired EmployeeServiceRepository employeeServiceRepository;
    @Autowired ServiceOptionRepository serviceOptionRepository;
    @Autowired JwtTokenService jwtTokenService;

    private Integer demoServiceId;
    private Integer demoOptionId;

    @BeforeEach
    void setUp() {
        clock.setBusinessTime(MORNING);
        notificationRepository.deleteAll();
        appointmentRepository.deleteAll();

        var demoAssignment = employeeServiceRepository.findAll().stream()
                .filter(item -> "DEMOEMP".equals(item.getEmployee().getEmployeeId()))
                .filter(item -> item.getServiceOption() != null)
                .findFirst().orElseThrow();
        demoServiceId = demoAssignment.getService().getServiceId();
        demoOptionId = demoAssignment.getServiceOption().getOptionId();
    }

    @Test
    void guestAppointmentAppearsInTheBookedDemoEmployeeScheduleAndNowhereElse() throws Exception {
        String created = mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(guestPayload())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeId").value("DEMOEMP"))
                .andExpect(jsonPath("$.data.customerName").value("Misafir"))
                .andExpect(jsonPath("$.data.customerSurname").value("Kullanici"))
                .andExpect(jsonPath("$.data.phone").isNotEmpty())
                .andExpect(jsonPath("$.data.price").isNumber())
                .andReturn().getResponse().getContentAsString();
        int appointmentId = objectMapper.readTree(created).path("data").path("appointmentId").asInt();

        mockMvc.perform(get("/api/appointments/employee/DEMOEMP")
                        .param("startDate", BOOKING_DATE.minusDays(1).toString())
                        .param("endDate", BOOKING_DATE.plusDays(6).toString())
                        .header("Authorization", "Bearer " + employeeToken("DEMOEMP")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.appointmentId == " + appointmentId + ")]").exists())
                .andExpect(jsonPath("$.data[?(@.appointmentId == " + appointmentId + ")].customerName")
                        .value("Misafir"))
                .andExpect(jsonPath("$.data[?(@.appointmentId == " + appointmentId + ")].serviceName")
                        .isNotEmpty());

        mockMvc.perform(get("/api/appointments/employee/GLW003")
                        .param("startDate", BOOKING_DATE.minusDays(1).toString())
                        .param("endDate", BOOKING_DATE.plusDays(6).toString())
                        .header("Authorization", "Bearer " + employeeToken("GLW003")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.appointmentId == " + appointmentId + ")]").doesNotExist());

        // Another employee cannot even query the booked employee's schedule.
        mockMvc.perform(get("/api/appointments/employee/DEMOEMP")
                        .param("startDate", BOOKING_DATE.minusDays(1).toString())
                        .param("endDate", BOOKING_DATE.plusDays(6).toString())
                        .header("Authorization", "Bearer " + employeeToken("GLW003")))
                .andExpect(status().isForbidden());
    }

    @Test
    void pastAppointmentStaysVisibleInTheEmployeeScheduleAfterItsTimePasses() throws Exception {
        String created = mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(guestPayload())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int appointmentId = objectMapper.readTree(created).path("data").path("appointmentId").asInt();

        clock.setBusinessTime(LocalDateTime.of(2026, 8, 14, 18, 0));

        mockMvc.perform(get("/api/appointments/employee/DEMOEMP")
                        .param("startDate", BOOKING_DATE.minusDays(1).toString())
                        .param("endDate", BOOKING_DATE.plusDays(6).toString())
                        .header("Authorization", "Bearer " + employeeToken("DEMOEMP")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.appointmentId == " + appointmentId + ")]").exists());
    }

    /**
     * Literal reproduction of the reported scenario: a customer books Defne Yılmaz
     * (GLW001, the seeded skin-care employee) for a specific date/time, then Defne
     * Yılmaz's own employee login queries her weekly schedule for that week and must
     * see it — using her real seeded competency/service data, not a synthetic employee.
     */
    @Test
    void customerBooksNamedEmployeeAndThatEmployeeSeesItInHerOwnWeeklySchedule() throws Exception {
        var defneAssignment = employeeServiceRepository.findAll().stream()
                .filter(item -> "GLW001".equals(item.getEmployee().getEmployeeId()))
                .findFirst().orElseThrow();
        Integer serviceId = defneAssignment.getService().getServiceId();
        Integer optionId = serviceOptionRepository
                .findByServiceServiceIdAndActiveTrueOrderByOptionNameAsc(serviceId)
                .getFirst().getOptionId();

        LocalDate appointmentDate = LocalDate.of(2026, 8, 18);
        clock.setBusinessTime(LocalDateTime.of(2026, 8, 17, 9, 0));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("employeeId", "GLW001");
        payload.put("serviceId", serviceId);
        payload.put("optionId", optionId);
        payload.put("customerName", "Test");
        payload.put("customerSurname", "Musteri");
        payload.put("phone", "05559998877");
        payload.put("appointmentDate", appointmentDate.toString());
        payload.put("appointmentTime", "12:00");

        String created = mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeId").value("GLW001"))
                .andReturn().getResponse().getContentAsString();
        int appointmentId = objectMapper.readTree(created).path("data").path("appointmentId").asInt();

        // Monday-start week containing 2026-08-18 (a Tuesday): 2026-08-17..2026-08-23.
        mockMvc.perform(get("/api/appointments/employee/GLW001")
                        .param("startDate", "2026-08-17")
                        .param("endDate", "2026-08-23")
                        .header("Authorization", "Bearer " + employeeToken("GLW001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.appointmentId == " + appointmentId + ")]").exists())
                .andExpect(jsonPath("$.data[?(@.appointmentId == " + appointmentId
                        + ")].appointmentDate").value("2026-08-18"))
                .andExpect(jsonPath("$.data[?(@.appointmentId == " + appointmentId
                        + ")].appointmentTime").value("12:00:00"));

        // A different employee cannot see it, and cannot query Defne's schedule either.
        mockMvc.perform(get("/api/appointments/employee/GLW001")
                        .param("startDate", "2026-08-17")
                        .param("endDate", "2026-08-23")
                        .header("Authorization", "Bearer " + employeeToken("GLW002")))
                .andExpect(status().isForbidden());
    }

    private Map<String, Object> guestPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("employeeId", "DEMOEMP");
        payload.put("serviceId", demoServiceId);
        payload.put("optionId", demoOptionId);
        payload.put("customerName", "Misafir");
        payload.put("customerSurname", "Kullanici");
        payload.put("phone", "05551234567");
        payload.put("appointmentDate", BOOKING_DATE.toString());
        payload.put("appointmentTime", "14:00");
        return payload;
    }

    private String employeeToken(String employeeId) {
        return jwtTokenService.generateToken(employeeId, UserRole.EMPLOYEE);
    }
}
