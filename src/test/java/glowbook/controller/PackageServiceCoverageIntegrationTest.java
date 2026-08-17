package glowbook.controller;

import glowbook.entity.Customer;
import glowbook.entity.CustomerPackage;
import glowbook.entity.ServiceOption;
import glowbook.entity.ServicePackage;
import glowbook.repository.AppointmentRepository;
import glowbook.repository.CustomerPackageRepository;
import glowbook.repository.CustomerRepository;
import glowbook.repository.NotificationRepository;
import glowbook.repository.ServiceOptionRepository;
import glowbook.repository.ServicePackageRepository;
import glowbook.repository.ServiceRepository;
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
 * Package/service coverage enforcement (KOMUT 2, defects A and E): a customer must never be
 * able to book a sub-service that a package does not actually cover, whether the mismatch
 * comes from the guided first-booking flow or from a hand-crafted request against an
 * already-owned package. Named after the exact regression scenario from the specification:
 * a Hydrafacial package must accept Hydrafacial and must never accept Akne Bakımı, even
 * though both belong to the same "Cilt Bakımı" service.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MutableClockTestConfig.class)
class PackageServiceCoverageIntegrationTest {

    /** Friday 14 August 2026, 09:00 Istanbul — before the 10:00 opening. */
    private static final LocalDateTime MORNING = LocalDateTime.of(2026, 8, 14, 9, 0);
    private static final LocalDate BOOKING_DATE = LocalDate.of(2026, 8, 14);
    private static final LocalTime BOOKING_TIME = LocalTime.of(11, 0);

    private static final String SKIN_EXPERT_ID = "GLW001";
    private static final String LASER_EXPERT_ID = "GLW002";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MutableClock clock;
    @Autowired CustomerRepository customerRepository;
    @Autowired CustomerPackageRepository customerPackageRepository;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired ServicePackageRepository servicePackageRepository;
    @Autowired ServiceRepository serviceRepository;
    @Autowired ServiceOptionRepository serviceOptionRepository;
    @Autowired JwtTokenService jwtTokenService;

    private Customer customer;
    private String customerToken;

    private ServicePackage hydrafacialPackage;
    private ServicePackage glowCiltPaketi;
    private Integer skinCareServiceId;
    private Integer laserServiceId;
    private Integer hydrafacialOptionId;
    private Integer akneBakimiOptionId;
    private Integer antiAgingOptionId;
    private Integer klasikCiltBakimiOptionId;
    private Integer lekeBakimiOptionId;
    private Integer laserOptionId;

    @BeforeEach
    void setUp() {
        clock.setBusinessTime(MORNING);
        notificationRepository.deleteAll();
        appointmentRepository.deleteAll();
        customerPackageRepository.deleteAll();

        customer = findOrCreateCustomer("paket-kapsam@glowbook.test", "Kapsam", "Musterisi", "05551239910");
        customerToken = jwtTokenService.generateToken(customer.getCustomerId().toString(), UserRole.CUSTOMER);

        hydrafacialPackage = findPackage("Hydrafacial Bakım Paketi");
        glowCiltPaketi = findPackage("Glow Cilt Paketi");
        skinCareServiceId = hydrafacialPackage.getService().getServiceId();
        hydrafacialOptionId = hydrafacialPackage.getCoveredOptions().iterator().next().getOptionId();
        akneBakimiOptionId = findOptionId(skinCareServiceId, "Akne Bakımı");
        antiAgingOptionId = findOptionId(skinCareServiceId, "Anti Aging Bakım");
        klasikCiltBakimiOptionId = findOptionId(skinCareServiceId, "Klasik Cilt Bakımı");
        lekeBakimiOptionId = findOptionId(skinCareServiceId, "Leke Bakımı");

        ServicePackage laserPackage = findPackage("5 Bölge Lazer Paketi");
        laserServiceId = laserPackage.getService().getServiceId();
        laserOptionId = laserPackage.getCoveredOptions().iterator().next().getOptionId();
    }

    // A. Hydrafacial package accepts its own covered Hydrafacial option.
    @Test
    void hydrafacialPackageAcceptsHydrafacialOption() throws Exception {
        mockMvc.perform(post(bookingPath(hydrafacialPackage))
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                bookingPayload(SKIN_EXPERT_ID, hydrafacialOptionId, BOOKING_DATE, BOOKING_TIME))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appointment.serviceId").value(skinCareServiceId))
                .andExpect(jsonPath("$.data.appointment.optionId").value(hydrafacialOptionId));
    }

    // B. Hydrafacial package rejects Akne Bakımı — the exact bug reported in the specification.
    @Test
    void hydrafacialPackageRejectsAkneBakimiOption() throws Exception {
        long packagesBefore = customerPackageRepository.count();

        mockMvc.perform(post(bookingPath(hydrafacialPackage))
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                bookingPayload(SKIN_EXPERT_ID, akneBakimiOptionId, BOOKING_DATE, BOOKING_TIME))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bu hizmet seçtiğin pakete dahil değil."));

        // The mismatched attempt must roll the package purchase back, not leave a half-bought package.
        assertThat(customerPackageRepository.count()).isEqualTo(packagesBefore);
        assertThat(appointmentRepository.count()).isZero();
    }

    // C. Single-option package requires no explicit choice: the option is derived silently.
    @Test
    void singleOptionPackageRequiresNoExplicitServiceChoice() throws Exception {
        Map<String, Object> payload = bookingPayload(SKIN_EXPERT_ID, null, BOOKING_DATE, BOOKING_TIME);
        mockMvc.perform(post(bookingPath(hydrafacialPackage))
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appointment.optionId").value(hydrafacialOptionId));
    }

    // D. Multi-option package accepts only its own included options, and still requires a
    // choice among them when more than one is covered.
    @Test
    void multiOptionPackageAcceptsOnlyIncludedOptions() throws Exception {
        // Accepts a genuinely covered option.
        mockMvc.perform(post(bookingPath(glowCiltPaketi))
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                bookingPayload(SKIN_EXPERT_ID, lekeBakimiOptionId, BOOKING_DATE, BOOKING_TIME))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appointment.optionId").value(lekeBakimiOptionId));

        // Rejects a same-service option that this specific package does not cover.
        Customer rival = findOrCreateCustomer("paket-kapsam-coklu@glowbook.test", "Coklu", "Musteri", "05551239911");
        String rivalToken = jwtTokenService.generateToken(rival.getCustomerId().toString(), UserRole.CUSTOMER);
        mockMvc.perform(post("/api/customers/" + rival.getCustomerId() + "/packages/" + glowCiltPaketi.getPackageId() + "/booking")
                        .header("Authorization", bearer(rivalToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                bookingPayload(SKIN_EXPERT_ID, antiAgingOptionId, BOOKING_DATE, BOOKING_TIME))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bu hizmet seçtiğin pakete dahil değil."));

        // A genuinely multi-option package cannot silently guess which one the customer meant.
        Customer thirdCustomer = findOrCreateCustomer("paket-kapsam-coklu2@glowbook.test", "UcuncuCoklu", "Musteri", "05551239912");
        String thirdToken = jwtTokenService.generateToken(thirdCustomer.getCustomerId().toString(), UserRole.CUSTOMER);
        mockMvc.perform(post("/api/customers/" + thirdCustomer.getCustomerId() + "/packages/" + glowCiltPaketi.getPackageId() + "/booking")
                        .header("Authorization", bearer(thirdToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                bookingPayload(SKIN_EXPERT_ID, null, BOOKING_DATE, BOOKING_TIME))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bu paket birden fazla seçenek kapsıyor. Lütfen birini seç."));
    }

    // E. An option from a wholly unrelated service is rejected exactly like a mismatched one.
    @Test
    void unrelatedServiceOptionRejected() throws Exception {
        mockMvc.perform(post(bookingPath(hydrafacialPackage))
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                bookingPayload(SKIN_EXPERT_ID, laserOptionId, BOOKING_DATE, BOOKING_TIME))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bu hizmet seçtiğin pakete dahil değil."));
    }

    // F. An employee who cannot perform the package's service is rejected, in Turkish.
    @Test
    void unqualifiedEmployeeRejected() throws Exception {
        long packagesBefore = customerPackageRepository.count();

        mockMvc.perform(post(bookingPath(hydrafacialPackage))
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                bookingPayload(LASER_EXPERT_ID, hydrafacialOptionId, BOOKING_DATE, BOOKING_TIME))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Seçilen personel bu hizmeti vermiyor."));

        assertThat(customerPackageRepository.count()).isEqualTo(packagesBefore);
    }

    // G. A package that belongs to a different customer can never be used to reserve a session.
    @Test
    void packageNotOwnedByCustomerRejected() throws Exception {
        Customer owner = findOrCreateCustomer("paket-sahibi@glowbook.test", "Sahip", "Musteri", "05551239913");
        String ownerToken = jwtTokenService.generateToken(owner.getCustomerId().toString(), UserRole.CUSTOMER);
        Integer ownedPackageId = purchasePlainPackage(owner.getCustomerId(), ownerToken, hydrafacialPackage.getPackageId());

        long appointmentsBefore = appointmentRepository.count();
        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                laterSessionPayload(customer.getCustomerId(), ownedPackageId, SKIN_EXPERT_ID,
                                        skinCareServiceId, hydrafacialOptionId, BOOKING_DATE, BOOKING_TIME))))
                .andExpect(status().isNotFound());

        assertThat(appointmentRepository.count()).isEqualTo(appointmentsBefore);
    }

    // H. Once every session of an owned package is used or scheduled, a further reservation is rejected.
    @Test
    void exhaustedPackageRejectedForLaterSession() throws Exception {
        Integer ownedPackageId = purchasePlainPackage(customer.getCustomerId(), customerToken, hydrafacialPackage.getPackageId());
        int totalSessions = hydrafacialPackage.getTotalSession();

        LocalDate date = BOOKING_DATE;
        for (int i = 0; i < totalSessions; i++) {
            date = nextWorkingDay(date);
            mockMvc.perform(post("/api/appointments")
                            .header("Authorization", bearer(customerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    laterSessionPayload(customer.getCustomerId(), ownedPackageId, SKIN_EXPERT_ID,
                                            skinCareServiceId, hydrafacialOptionId, date, BOOKING_TIME))))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                laterSessionPayload(customer.getCustomerId(), ownedPackageId, SKIN_EXPERT_ID,
                                        skinCareServiceId, hydrafacialOptionId, nextWorkingDay(date), BOOKING_TIME))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Paketinde planlanabilir seans kalmadı."));
    }

    // I. A deactivated package can never back a new reservation.
    @Test
    void inactivePackageRejectedForLaterSession() throws Exception {
        Integer ownedPackageId = purchasePlainPackage(customer.getCustomerId(), customerToken, hydrafacialPackage.getPackageId());
        CustomerPackage owned = customerPackageRepository.findById(ownedPackageId).orElseThrow();
        owned.setActive(false);
        customerPackageRepository.save(owned);

        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                laterSessionPayload(customer.getCustomerId(), ownedPackageId, SKIN_EXPERT_ID,
                                        skinCareServiceId, hydrafacialOptionId, BOOKING_DATE, BOOKING_TIME))))
                .andExpect(status().isNotFound());
    }

    // J. Session accounting (total/used/scheduled/remaining) stays correct through the coverage-checked path.
    @Test
    void sessionAccountingPreservedThroughCoverageCheckedFirstBooking() throws Exception {
        mockMvc.perform(post(bookingPath(hydrafacialPackage))
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                bookingPayload(SKIN_EXPERT_ID, hydrafacialOptionId, BOOKING_DATE, BOOKING_TIME))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/customers/" + customer.getCustomerId() + "/packages")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].totalSession").value(hydrafacialPackage.getTotalSession()))
                .andExpect(jsonPath("$.data[0].usedSession").value(0))
                .andExpect(jsonPath("$.data[0].scheduledSession").value(1))
                .andExpect(jsonPath("$.data[0].remainingSession").value(hydrafacialPackage.getTotalSession() - 1));
    }

    // K. Cancelling a coverage-checked booking restores exactly one session, same as any other package.
    @Test
    void cancellingCoverageCheckedBookingRestoresExactlyOneSession() throws Exception {
        String response = mockMvc.perform(post(bookingPath(hydrafacialPackage))
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                bookingPayload(SKIN_EXPERT_ID, hydrafacialOptionId, BOOKING_DATE, BOOKING_TIME))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int appointmentId = objectMapper.readTree(response)
                .path("data").path("appointment").path("appointmentId").asInt();

        mockMvc.perform(patch("/api/appointments/" + appointmentId + "/cancel")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cancellationReason\":\"Vazgectim\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/customers/" + customer.getCustomerId() + "/packages")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].usedSession").value(0))
                .andExpect(jsonPath("$.data[0].scheduledSession").value(0))
                .andExpect(jsonPath("$.data[0].remainingSession").value(hydrafacialPackage.getTotalSession()));
    }

    // L. Full first-appointment regression: Hydrafacial is bookable, Akne Bakımı never is,
    // even against the same freshly purchased package.
    @Test
    void firstPackageAppointmentRegressionNeverAllowsAkneBakimi() throws Exception {
        mockMvc.perform(post(bookingPath(hydrafacialPackage))
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                bookingPayload(SKIN_EXPERT_ID, akneBakimiOptionId, BOOKING_DATE, BOOKING_TIME))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bu hizmet seçtiğin pakete dahil değil."));

        mockMvc.perform(post(bookingPath(hydrafacialPackage))
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                bookingPayload(SKIN_EXPERT_ID, hydrafacialOptionId, BOOKING_DATE, BOOKING_TIME))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appointment.optionId").value(hydrafacialOptionId));
    }

    // M. Later package session regression: a hand-crafted mismatch is rejected even against
    // an already-owned package, while the genuinely covered option still works.
    @Test
    void laterPackageSessionRegressionRejectsCraftedMismatch() throws Exception {
        Integer ownedPackageId = purchasePlainPackage(customer.getCustomerId(), customerToken, hydrafacialPackage.getPackageId());

        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                laterSessionPayload(customer.getCustomerId(), ownedPackageId, SKIN_EXPERT_ID,
                                        skinCareServiceId, akneBakimiOptionId, BOOKING_DATE, BOOKING_TIME))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bu hizmet seçtiğin pakete dahil değil."));

        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                laterSessionPayload(customer.getCustomerId(), ownedPackageId, SKIN_EXPERT_ID,
                                        skinCareServiceId, hydrafacialOptionId, BOOKING_DATE, BOOKING_TIME))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.optionId").value(hydrafacialOptionId));
    }

    private Integer purchasePlainPackage(Integer customerId, String token, Integer packageId) throws Exception {
        String response = mockMvc.perform(post("/api/customers/" + customerId + "/packages/" + packageId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("customerPackageId").asInt();
    }

    private LocalDate nextWorkingDay(LocalDate date) {
        LocalDate next = date.plusDays(1);
        while (next.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            next = next.plusDays(1);
        }
        return next;
    }

    private ServicePackage findPackage(String name) {
        return servicePackageRepository.findByActiveTrueOrderByPackageNameAsc().stream()
                .filter(item -> name.equals(item.getPackageName()))
                .findFirst().orElseThrow();
    }

    private Integer findOptionId(Integer serviceId, String optionName) {
        return serviceOptionRepository.findByServiceServiceIdAndActiveTrueOrderByOptionNameAsc(serviceId).stream()
                .filter(option -> optionName.equals(option.getOptionName()))
                .map(ServiceOption::getOptionId)
                .findFirst().orElseThrow();
    }

    /** The Spring context is shared across test classes, so reuse rather than re-insert. */
    private Customer findOrCreateCustomer(String email, String firstName, String lastName, String phone) {
        return customerRepository.findByEmail(email)
                .orElseGet(() -> customerRepository.save(Customer.builder()
                        .firstName(firstName).lastName(lastName)
                        .phone(phone).email(email)
                        .password("irrelevant").active(true).build()));
    }

    private Map<String, Object> bookingPayload(String employeeId, Integer optionId, LocalDate date, LocalTime time) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("employeeId", employeeId);
        payload.put("optionId", optionId);
        payload.put("appointmentDate", date.toString());
        payload.put("appointmentTime", time.toString());
        return payload;
    }

    private Map<String, Object> laterSessionPayload(Integer customerId, Integer customerPackageId, String employeeId,
                                                      Integer serviceId, Integer optionId, LocalDate date, LocalTime time) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("customerId", customerId);
        payload.put("customerPackageId", customerPackageId);
        payload.put("employeeId", employeeId);
        payload.put("serviceId", serviceId);
        payload.put("optionId", optionId);
        payload.put("appointmentDate", date.toString());
        payload.put("appointmentTime", time.toString());
        return payload;
    }

    private String bookingPath(ServicePackage servicePackage) {
        return "/api/customers/" + customer.getCustomerId() + "/packages/" + servicePackage.getPackageId() + "/booking";
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
