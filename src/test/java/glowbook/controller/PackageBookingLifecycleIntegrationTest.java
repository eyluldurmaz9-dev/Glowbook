package glowbook.controller;

import glowbook.entity.Customer;
import glowbook.entity.ServicePackage;
import glowbook.repository.AppointmentRepository;
import glowbook.repository.CustomerPackageRepository;
import glowbook.repository.CustomerRepository;
import glowbook.repository.EmployeeServiceRepository;
import glowbook.repository.NotificationRepository;
import glowbook.repository.ServicePackageRepository;
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
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end scenario from section 21 of the specification, driven by a controllable clock:
 * buy a 10-session package, book its first appointment, verify Paketlerim counts, verify the
 * booked employee sees it, advance the clock past the appointment and verify it moves to
 * history and consumes exactly one session.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MutableClockTestConfig.class)
class PackageBookingLifecycleIntegrationTest {

    /** Friday 14 August 2026, 09:00 Istanbul — before the 10:00 opening. */
    private static final LocalDateTime MORNING = LocalDateTime.of(2026, 8, 14, 9, 0);
    private static final LocalDate BOOKING_DATE = LocalDate.of(2026, 8, 14);
    private static final LocalTime BOOKING_TIME = LocalTime.of(14, 0);

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MutableClock clock;
    @Autowired CustomerRepository customerRepository;
    @Autowired CustomerPackageRepository customerPackageRepository;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired ServicePackageRepository servicePackageRepository;
    @Autowired EmployeeServiceRepository employeeServiceRepository;
    @Autowired JwtTokenService jwtTokenService;

    private Customer customer;
    private String customerToken;
    private ServicePackage tenSessionPackage;
    private Integer optionId;
    private String bookedEmployeeId;
    private String otherEmployeeId;

    @BeforeEach
    void setUp() {
        clock.setBusinessTime(MORNING);
        notificationRepository.deleteAll();
        appointmentRepository.deleteAll();
        customerPackageRepository.deleteAll();

        customer = findOrCreateCustomer("paket-lifecycle@glowbook.test", "Paket", "Musterisi", "05551239900");
        customerToken = jwtTokenService.generateToken(customer.getCustomerId().toString(), UserRole.CUSTOMER);

        tenSessionPackage = servicePackageRepository.findByActiveTrueOrderByPackageNameAsc().stream()
                .filter(item -> item.getTotalSession() != null && item.getTotalSession() == 10)
                .findFirst().orElseThrow();
        Integer serviceId = tenSessionPackage.getService().getServiceId();
        // Must be one of the package's own covered options, not just any option under its
        // service — the whole point of package/service coverage is that those can differ.
        optionId = tenSessionPackage.getCoveredOptions().iterator().next().getOptionId();

        bookedEmployeeId = employeeServiceRepository.findAll().stream()
                .filter(item -> item.getService().getServiceId().equals(serviceId))
                .filter(item -> Boolean.TRUE.equals(item.getEmployee().getActive()))
                .map(item -> item.getEmployee().getEmployeeId())
                .findFirst().orElseThrow();
        otherEmployeeId = employeeServiceRepository.findAll().stream()
                .map(item -> item.getEmployee().getEmployeeId())
                .filter(id -> !id.equals(bookedEmployeeId))
                .findFirst().orElseThrow();
    }

    @Test
    void packagePurchaseWithFirstBookingKeepsCountsConsistentAsTimePasses() throws Exception {
        // 1-8. One call carries the already known package/service forward and creates exactly one appointment.
        long appointmentsBefore = appointmentRepository.count();
        String bookingResponse = mockMvc.perform(post(bookingPath())
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingPayload())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appointment.appointmentId").isNumber())
                .andExpect(jsonPath("$.data.appointment.employeeId").value(bookedEmployeeId))
                .andExpect(jsonPath("$.data.appointment.appointmentTime").value("14:00:00"))
                .andReturn().getResponse().getContentAsString();

        assertThat(appointmentRepository.count()).isEqualTo(appointmentsBefore + 1);
        int appointmentId = objectMapper.readTree(bookingResponse)
                .path("data").path("appointment").path("appointmentId").asInt();

        // The request never carried a serviceId: it is derived from the package.
        assertThat(objectMapper.readTree(bookingResponse)
                .path("data").path("appointment").path("serviceId").asInt())
                .isEqualTo(tenSessionPackage.getService().getServiceId());

        // 9. Paketlerim: total 10, used 0, scheduled 1, remaining 9.
        expectPackageCounts(10, 0, 1, 9);

        // 10. The appointment is upcoming and not in history.
        mockMvc.perform(get(upcomingPath()).header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].appointmentId").value(appointmentId));
        mockMvc.perform(get(pastPath()).header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        // 11-12. The booked employee sees the very same appointment in the weekly schedule.
        mockMvc.perform(get("/api/appointments/employee/" + bookedEmployeeId)
                        .param("startDate", BOOKING_DATE.minusDays(3).toString())
                        .param("endDate", BOOKING_DATE.plusDays(3).toString())
                        .header("Authorization", bearer(employeeToken(bookedEmployeeId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.appointmentId == " + appointmentId + ")]").exists());

        // Another employee must not see it, enforced by the backend.
        mockMvc.perform(get("/api/appointments/employee/" + otherEmployeeId)
                        .param("startDate", BOOKING_DATE.minusDays(3).toString())
                        .param("endDate", BOOKING_DATE.plusDays(3).toString())
                        .header("Authorization", bearer(employeeToken(otherEmployeeId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.appointmentId == " + appointmentId + ")]").doesNotExist());

        // 13. Advance the business clock past the appointment.
        clock.setBusinessTime(LocalDateTime.of(2026, 8, 14, 15, 0));

        // 14. It is now history only — no database status change was needed.
        mockMvc.perform(get(upcomingPath()).header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
        mockMvc.perform(get(pastPath()).header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].appointmentId").value(appointmentId));

        // 15. Paketlerim: used 1, scheduled 0, remaining still 9.
        expectPackageCounts(10, 1, 0, 9);

        // 16. The employee's schedule still contains the same single record.
        mockMvc.perform(get("/api/appointments/employee/" + bookedEmployeeId)
                        .param("startDate", BOOKING_DATE.minusDays(3).toString())
                        .param("endDate", BOOKING_DATE.plusDays(3).toString())
                        .header("Authorization", bearer(employeeToken(bookedEmployeeId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.appointmentId == " + appointmentId + ")]").exists());
        assertThat(appointmentRepository.count()).isEqualTo(appointmentsBefore + 1);
    }

    @Test
    void cancellingTheFirstAppointmentReleasesTheSessionExactlyOnce() throws Exception {
        String bookingResponse = mockMvc.perform(post(bookingPath())
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingPayload())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int appointmentId = objectMapper.readTree(bookingResponse)
                .path("data").path("appointment").path("appointmentId").asInt();

        expectPackageCounts(10, 0, 1, 9);

        mockMvc.perform(patch("/api/appointments/" + appointmentId + "/cancel")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cancellationReason\":\"Plan degisti\"}"))
                .andExpect(status().isOk());

        // Session restored once, not twice.
        expectPackageCounts(10, 0, 0, 10);

        // A repeated cancel is a no-op and must not restore a second session.
        mockMvc.perform(patch("/api/appointments/" + appointmentId + "/cancel")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cancellationReason\":\"Plan degisti\"}"))
                .andExpect(status().isOk());
        expectPackageCounts(10, 0, 0, 10);

        // Cancelled future appointment is not upcoming.
        mockMvc.perform(get(upcomingPath()).header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void failedFirstBookingRollsBackThePackagePurchase() throws Exception {
        long packagesBefore = customerPackageRepository.count();

        Map<String, Object> payload = bookingPayload();
        payload.put("appointmentTime", "14:30");
        mockMvc.perform(post(bookingPath())
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Randevu saati tam saat olmalıdır (örnek: 10:00)."));

        assertThat(customerPackageRepository.count()).isEqualTo(packagesBefore);
        assertThat(appointmentRepository.count()).isZero();

        Map<String, Object> pastPayload = bookingPayload();
        pastPayload.put("appointmentDate", BOOKING_DATE.minusDays(1).toString());
        mockMvc.perform(post(bookingPath())
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pastPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Geçmiş bir tarih için randevu oluşturamazsın."));

        assertThat(customerPackageRepository.count()).isEqualTo(packagesBefore);
        assertThat(appointmentRepository.count()).isZero();
    }

    @Test
    void packageBookingRejectsAPastSameDayHourAndDuplicateSlots() throws Exception {
        clock.setBusinessTime(LocalDateTime.of(2026, 8, 14, 14, 20));

        Map<String, Object> payload = bookingPayload();
        mockMvc.perform(post(bookingPath())
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bu saat artık geçmişte kaldı. Lütfen başka bir saat seç."));

        clock.setBusinessTime(MORNING);
        mockMvc.perform(post(bookingPath())
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingPayload())))
                .andExpect(status().isOk());

        // A second customer cannot take the same employee/slot.
        Customer rival = findOrCreateCustomer("paket-rakip@glowbook.test", "Rakip", "Musteri", "05551239901");
        Map<String, Object> rivalPayload = bookingPayload();
        mockMvc.perform(post("/api/customers/" + rival.getCustomerId()
                        + "/packages/" + tenSessionPackage.getPackageId() + "/booking")
                        .header("Authorization", bearer(
                                jwtTokenService.generateToken(rival.getCustomerId().toString(), UserRole.CUSTOMER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rivalPayload)))
                .andExpect(status().isConflict());
    }

    @Test
    void customerFacingMessagesStayFreeOfTechnicalWording() throws Exception {
        Map<String, Object> payload = bookingPayload();
        payload.put("appointmentTime", "14:30");
        String message = mockMvc.perform(post(bookingPath())
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(message.toLowerCase())
                .doesNotContain("endpoint")
                .doesNotContain("http")
                .doesNotContain("json")
                .doesNotContain("dto")
                .doesNotContain("database")
                .doesNotContain("sqlexception");
    }

    private void expectPackageCounts(int total, int used, int scheduled, int remaining) throws Exception {
        mockMvc.perform(get("/api/customers/" + customer.getCustomerId() + "/packages")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].totalSession").value(total))
                .andExpect(jsonPath("$.data[0].usedSession").value(used))
                .andExpect(jsonPath("$.data[0].scheduledSession").value(scheduled))
                .andExpect(jsonPath("$.data[0].remainingSession").value(remaining));
    }

    /** The Spring context is shared across test classes, so reuse rather than re-insert. */
    private Customer findOrCreateCustomer(String email, String firstName, String lastName, String phone) {
        return customerRepository.findByEmail(email)
                .orElseGet(() -> customerRepository.save(Customer.builder()
                        .firstName(firstName).lastName(lastName)
                        .phone(phone).email(email)
                        .password("irrelevant").active(true).build()));
    }

    private Map<String, Object> bookingPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("employeeId", bookedEmployeeId);
        payload.put("optionId", optionId);
        payload.put("appointmentDate", BOOKING_DATE.toString());
        payload.put("appointmentTime", BOOKING_TIME.toString());
        return payload;
    }

    private String bookingPath() {
        return "/api/customers/" + customer.getCustomerId()
                + "/packages/" + tenSessionPackage.getPackageId() + "/booking";
    }

    private String upcomingPath() {
        return "/api/appointments/customer/" + customer.getCustomerId() + "/upcoming";
    }

    private String pastPath() {
        return "/api/appointments/customer/" + customer.getCustomerId() + "/past";
    }

    private String employeeToken(String employeeId) {
        return jwtTokenService.generateToken(employeeId, UserRole.EMPLOYEE);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
