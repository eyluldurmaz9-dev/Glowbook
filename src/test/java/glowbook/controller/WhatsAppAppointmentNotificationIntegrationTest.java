package glowbook.controller;

import glowbook.entity.Customer;
import glowbook.entity.Notification;
import glowbook.entity.NotificationChannel;
import glowbook.entity.NotificationDeliveryStatus;
import glowbook.entity.NotificationType;
import glowbook.entity.ServicePackage;
import glowbook.repository.AppointmentRepository;
import glowbook.repository.CustomerPackageRepository;
import glowbook.repository.CustomerRepository;
import glowbook.repository.EmployeeServiceRepository;
import glowbook.repository.NotificationRepository;
import glowbook.repository.ServiceOptionRepository;
import glowbook.repository.ServicePackageRepository;
import glowbook.security.JwtTokenService;
import glowbook.security.UserRole;
import glowbook.support.FakeWhatsAppSender;
import glowbook.support.MutableClock;
import glowbook.support.MutableClockTestConfig;
import glowbook.support.WhatsAppTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers spec section 20 (A-L): a successfully created appointment — registered, guest,
 * package-first, or a later package booking — triggers exactly one WhatsApp confirmation
 * with the real employee/date/time/service, using a normalized Turkish phone; a failed or
 * rolled-back booking never sends one; a provider failure never touches the appointment;
 * and a duplicate slot attempt never produces a second message. Item M (webhook status
 * handling) lives in {@link WhatsAppWebhookControllerIntegrationTest}; item I (disabled
 * provider makes no external request) lives in {@code LoggingWhatsAppSenderTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({MutableClockTestConfig.class, WhatsAppTestConfig.class})
class WhatsAppAppointmentNotificationIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MutableClock clock;
    @Autowired FakeWhatsAppSender whatsAppSender;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired EmployeeServiceRepository employeeServiceRepository;
    @Autowired ServiceOptionRepository serviceOptionRepository;
    @Autowired ServicePackageRepository servicePackageRepository;
    @Autowired CustomerPackageRepository customerPackageRepository;
    @Autowired CustomerRepository customerRepository;
    @Autowired JwtTokenService jwtTokenService;

    private LocalDate workingDate;
    private String employeeId;
    private String employeeFullName;
    private Integer serviceId;
    private String serviceName;
    private Integer optionId;

    @BeforeEach
    void setUp() {
        whatsAppSender.reset();
        notificationRepository.deleteAll();
        appointmentRepository.deleteAll();
        customerPackageRepository.deleteAll();
        clock.setBusinessTime(LocalDateTime.of(2026, 8, 14, 9, 0));

        workingDate = LocalDate.of(2026, 8, 17); // Monday
        while (workingDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            workingDate = workingDate.plusDays(1);
        }

        var assignment = employeeServiceRepository.findAll().stream()
                .filter(item -> "GLW001".equals(item.getEmployee().getEmployeeId()))
                .findFirst().orElseThrow();
        employeeId = assignment.getEmployee().getEmployeeId();
        employeeFullName = assignment.getEmployee().getFirstName() + " " + assignment.getEmployee().getLastName();
        serviceId = assignment.getService().getServiceId();
        serviceName = assignment.getService().getServiceName();
        optionId = serviceOptionRepository.findByServiceServiceIdAndActiveTrueOrderByOptionNameAsc(serviceId)
                .getFirst().getOptionId();
    }

    // --- A: registered customer booking -------------------------------------------------

    @Test
    void registeredBookingTriggersExactlyOneWhatsAppSendWithRealDetails() throws Exception {
        Customer customer = customerRepository.save(Customer.builder()
                .firstName("Derya").lastName("Musteri").phone("05551230001")
                .email("derya-whatsapp@glowbook.test").password("irrelevant").active(true).build());

        Map<String, Object> payload = basePayload();
        payload.put("customerId", customer.getCustomerId());
        payload.remove("customerName");
        payload.remove("customerSurname");
        payload.remove("phone");

        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", bearer(jwtTokenService.generateToken(
                                customer.getCustomerId().toString(), UserRole.CUSTOMER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        assertThat(whatsAppSender.sendCount()).isEqualTo(1);
        FakeWhatsAppSender.RecordedSend send = whatsAppSender.sends().getFirst();
        assertThat(send.phone()).isEqualTo("+905551230001");
        assertThat(send.templateName()).isEqualTo("appointment_confirmation_tr");
        assertThat(send.languageCode()).isEqualTo("tr");
        assertThat(send.parameters()).containsExactly("17 Ağustos 2026", "10:00", serviceName, employeeFullName);

        Notification notification = onlyNotification();
        assertThat(notification.getChannel()).isEqualTo(NotificationChannel.WHATSAPP);
        assertThat(notification.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.SENT);
        assertThat(notification.getProviderMessageId()).isEqualTo("fake-message-1");
        assertThat(notification.getSentAt()).isNotNull();
    }

    // --- B: guest booking -----------------------------------------------------------------

    @Test
    void guestBookingTriggersExactlyOneWhatsAppSend() throws Exception {
        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(basePayload())))
                .andExpect(status().isOk());

        assertThat(whatsAppSender.sendCount()).isEqualTo(1);
        assertThat(whatsAppSender.sends().getFirst().phone()).isEqualTo("+905551234567");

        Notification notification = onlyNotification();
        assertThat(notification.getChannel()).isEqualTo(NotificationChannel.WHATSAPP);
        assertThat(notification.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.SENT);
    }

    // --- C & D: package purchase + first appointment, then a later session booking --------

    @Test
    void packageFirstAppointmentAndLaterSessionBookingEachTriggerOneWhatsAppSend() throws Exception {
        Customer customer = customerRepository.save(Customer.builder()
                .firstName("Paket").lastName("Musteri").phone("05551230002")
                .email("paket-whatsapp@glowbook.test").password("irrelevant").active(true).build());
        String token = jwtTokenService.generateToken(customer.getCustomerId().toString(), UserRole.CUSTOMER);

        ServicePackage tenSession = servicePackageRepository.findByActiveTrueOrderByPackageNameAsc().stream()
                .filter(item -> item.getTotalSession() != null && item.getTotalSession() == 10)
                .findFirst().orElseThrow();
        Integer packageServiceId = tenSession.getService().getServiceId();
        // Must be one of the package's own covered options, not just any option under its
        // service — the whole point of package/service coverage is that those can differ.
        Integer packageOptionId = tenSession.getCoveredOptions().iterator().next().getOptionId();
        String packageEmployeeId = employeeServiceRepository.findAll().stream()
                .filter(item -> item.getService().getServiceId().equals(packageServiceId))
                .filter(item -> Boolean.TRUE.equals(item.getEmployee().getActive()))
                .map(item -> item.getEmployee().getEmployeeId())
                .findFirst().orElseThrow();

        // C. Package purchase + first appointment.
        Map<String, Object> firstBookingPayload = new LinkedHashMap<>();
        firstBookingPayload.put("employeeId", packageEmployeeId);
        firstBookingPayload.put("optionId", packageOptionId);
        firstBookingPayload.put("appointmentDate", workingDate.toString());
        firstBookingPayload.put("appointmentTime", "11:00");

        String firstResponse = mockMvc.perform(post("/api/customers/" + customer.getCustomerId()
                        + "/packages/" + tenSession.getPackageId() + "/booking")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstBookingPayload)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(whatsAppSender.sendCount()).isEqualTo(1);
        assertThat(whatsAppSender.sends().getFirst().parameters()).contains(
                tenSession.getService().getServiceName());

        int firstAppointmentId = objectMapper.readTree(firstResponse)
                .path("data").path("appointment").path("appointmentId").asInt();
        Integer customerPackageId = objectMapper.readTree(firstResponse)
                .path("data").path("customerPackage").path("customerPackageId").asInt();

        // D. A later booking against the same package, via the normal appointment endpoint.
        Map<String, Object> laterBookingPayload = new LinkedHashMap<>();
        laterBookingPayload.put("customerId", customer.getCustomerId());
        laterBookingPayload.put("customerPackageId", customerPackageId);
        laterBookingPayload.put("employeeId", packageEmployeeId);
        laterBookingPayload.put("serviceId", packageServiceId);
        laterBookingPayload.put("optionId", packageOptionId);
        laterBookingPayload.put("appointmentDate", workingDate.plusDays(1).toString());
        laterBookingPayload.put("appointmentTime", "11:00");

        String laterResponse = mockMvc.perform(post("/api/appointments")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(laterBookingPayload)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int laterAppointmentId = objectMapper.readTree(laterResponse).path("data").path("appointmentId").asInt();
        assertThat(laterAppointmentId).isNotEqualTo(firstAppointmentId);

        assertThat(whatsAppSender.sendCount()).isEqualTo(2);
        assertThat(notificationRepository.count()).isEqualTo(2);
    }

    // --- E: Turkish phone normalization -----------------------------------------------------

    @Test
    void variousRawPhoneFormatsAreNormalizedBeforeSending() throws Exception {
        Map<String, Object> payload = basePayload();
        payload.put("phone", "0 555 123 45 67");

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        assertThat(whatsAppSender.sends().getFirst().phone()).isEqualTo("+905551234567");
    }

    // --- F: rejected booking sends nothing ----------------------------------------------

    @Test
    void rejectedAppointmentSendsNoWhatsAppMessage() throws Exception {
        Map<String, Object> payload = basePayload();
        payload.put("appointmentTime", "10:30"); // half-hour starts are rejected

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());

        assertThat(whatsAppSender.sendCount()).isZero();
        assertThat(notificationRepository.count()).isZero();
    }

    // --- G: rolled-back package purchase sends nothing ------------------------------------

    @Test
    void rolledBackPackagePurchaseSendsNoWhatsAppMessage() throws Exception {
        Customer customer = customerRepository.save(Customer.builder()
                .firstName("Iptal").lastName("Musteri").phone("05551230003")
                .email("iptal-whatsapp@glowbook.test").password("irrelevant").active(true).build());
        String token = jwtTokenService.generateToken(customer.getCustomerId().toString(), UserRole.CUSTOMER);

        ServicePackage tenSession = servicePackageRepository.findByActiveTrueOrderByPackageNameAsc().stream()
                .filter(item -> item.getTotalSession() != null && item.getTotalSession() == 10)
                .findFirst().orElseThrow();
        Integer packageServiceId = tenSession.getService().getServiceId();
        Integer packageOptionId = tenSession.getCoveredOptions().iterator().next().getOptionId();
        String packageEmployeeId = employeeServiceRepository.findAll().stream()
                .filter(item -> item.getService().getServiceId().equals(packageServiceId))
                .filter(item -> Boolean.TRUE.equals(item.getEmployee().getActive()))
                .map(item -> item.getEmployee().getEmployeeId())
                .findFirst().orElseThrow();

        long packagesBefore = customerPackageRepository.count();

        Map<String, Object> invalidPayload = new LinkedHashMap<>();
        invalidPayload.put("employeeId", packageEmployeeId);
        invalidPayload.put("optionId", packageOptionId);
        invalidPayload.put("appointmentDate", workingDate.toString());
        invalidPayload.put("appointmentTime", "11:30"); // half-hour -> rejected -> whole purchase rolls back

        mockMvc.perform(post("/api/customers/" + customer.getCustomerId()
                        + "/packages/" + tenSession.getPackageId() + "/booking")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());

        assertThat(customerPackageRepository.count()).isEqualTo(packagesBefore);
        assertThat(whatsAppSender.sendCount()).isZero();
        assertThat(notificationRepository.count()).isZero();
    }

    // --- H: provider failure never touches the appointment --------------------------------

    @Test
    void whatsAppProviderFailurePreservesTheAppointment() throws Exception {
        whatsAppSender.failNextSend("Simulated Cloud API outage");

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(basePayload())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appointmentId").isNumber());

        assertThat(appointmentRepository.count()).isEqualTo(1);
        assertThat(whatsAppSender.sendCount()).isEqualTo(1);

        Notification notification = onlyNotification();
        assertThat(notification.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.FAILED);
        assertThat(notification.getFailedAt()).isNotNull();
        assertThat(notification.getProviderMessageId()).isNull();
    }

    // --- J: duplicate slot submission sends at most one message ---------------------------

    @Test
    void duplicateSlotSubmissionSendsAtMostOneWhatsAppMessage() throws Exception {
        String payload = objectMapper.writeValueAsString(basePayload());

        mockMvc.perform(post("/api/appointments").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/appointments").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isConflict());

        assertThat(whatsAppSender.sendCount()).isEqualTo(1);
    }

    private Notification onlyNotification() {
        assertThat(notificationRepository.count()).isEqualTo(1);
        return notificationRepository.findAll().getFirst();
    }

    private Map<String, Object> basePayload() {
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

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
