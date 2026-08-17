package glowbook.controller;

import glowbook.repository.AppointmentRepository;
import glowbook.repository.EmployeeServiceRepository;
import glowbook.repository.NotificationRepository;
import glowbook.repository.ServiceOptionRepository;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spec item I / the current desired production configuration: with {@code
 * WHATSAPP_ENABLED=false} (the default — no real salon uses GlowBook yet), a successful
 * booking must still work normally, but must not attempt a WhatsApp send at all, not even
 * through the no-op logging provider, and must not create a WhatsApp notification row.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({MutableClockTestConfig.class, WhatsAppTestConfig.class})
@TestPropertySource(properties = "app.whatsapp.enabled=false")
class WhatsAppDisabledIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MutableClock clock;
    @Autowired FakeWhatsAppSender whatsAppSender;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired EmployeeServiceRepository employeeServiceRepository;
    @Autowired ServiceOptionRepository serviceOptionRepository;

    @BeforeEach
    void setUp() {
        whatsAppSender.reset();
        notificationRepository.deleteAll();
        appointmentRepository.deleteAll();
        clock.setBusinessTime(LocalDateTime.of(2026, 8, 14, 9, 0));
    }

    @Test
    void disabledWhatsAppStillAllowsBookingButSendsNothing() throws Exception {
        var assignment = employeeServiceRepository.findAll().stream()
                .filter(item -> "GLW001".equals(item.getEmployee().getEmployeeId()))
                .findFirst().orElseThrow();
        Integer serviceId = assignment.getService().getServiceId();
        Integer optionId = serviceOptionRepository.findByServiceServiceIdAndActiveTrueOrderByOptionNameAsc(serviceId)
                .getFirst().getOptionId();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("employeeId", assignment.getEmployee().getEmployeeId());
        payload.put("serviceId", serviceId);
        payload.put("optionId", optionId);
        payload.put("customerName", "Devre");
        payload.put("customerSurname", "Disi");
        payload.put("phone", "05551230098");
        payload.put("appointmentDate", LocalDate.of(2026, 8, 17).toString());
        payload.put("appointmentTime", "10:00");

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        assertThat(appointmentRepository.count()).isEqualTo(1);
        assertThat(whatsAppSender.sendCount()).isZero();
        assertThat(notificationRepository.count()).isZero();
    }
}
