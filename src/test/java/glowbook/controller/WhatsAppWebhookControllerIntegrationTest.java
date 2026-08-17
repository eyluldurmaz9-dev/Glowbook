package glowbook.controller;

import glowbook.entity.Notification;
import glowbook.entity.NotificationDeliveryStatus;
import glowbook.repository.AppointmentRepository;
import glowbook.repository.NotificationRepository;
import glowbook.repository.EmployeeServiceRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers spec section 20 item M: Meta's subscription handshake and delivery-status
 * callbacks. A send-accepted response only ever proves SENT — this exercises the webhook
 * as the only path that can promote a notification to DELIVERED/READ.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({MutableClockTestConfig.class, WhatsAppTestConfig.class})
class WhatsAppWebhookControllerIntegrationTest {

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
    void verifyEchoesChallengeOnlyWhenTokenMatches() throws Exception {
        mockMvc.perform(get("/api/whatsapp/webhook")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "not-the-configured-token")
                        .param("hub.challenge", "12345"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/whatsapp/webhook")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "test-verify-token")
                        .param("hub.challenge", "12345"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string("12345"));
    }

    @Test
    void statusCallbackPromotesNotificationToDeliveredThenRead() throws Exception {
        int appointmentId = createAppointmentAndReturnId();
        String messageId = onlyNotification(appointmentId).getProviderMessageId();
        assertThat(messageId).isNotNull();

        postStatus(messageId, "delivered");
        Notification afterDelivered = onlyNotification(appointmentId);
        assertThat(afterDelivered.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.DELIVERED);
        assertThat(afterDelivered.getDeliveredAt()).isNotNull();

        postStatus(messageId, "read");
        Notification afterRead = onlyNotification(appointmentId);
        assertThat(afterRead.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.READ);
        assertThat(afterRead.getReadAt()).isNotNull();
    }

    @Test
    void outOfOrderStatusCallbackDoesNotRegressAnAlreadyMoreAdvancedStatus() throws Exception {
        int appointmentId = createAppointmentAndReturnId();
        String messageId = onlyNotification(appointmentId).getProviderMessageId();

        postStatus(messageId, "read");
        postStatus(messageId, "sent"); // stale/duplicate webhook arriving late

        Notification notification = onlyNotification(appointmentId);
        assertThat(notification.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.READ);
    }

    @Test
    void unknownMessageIdIsIgnoredWithoutError() throws Exception {
        postStatus("no-such-message-id", "delivered").andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions postStatus(String messageId, String status) throws Exception {
        Map<String, Object> statusEntry = new LinkedHashMap<>();
        statusEntry.put("id", messageId);
        statusEntry.put("status", status);

        Map<String, Object> value = Map.of("statuses", List.of(statusEntry));
        Map<String, Object> change = Map.of("value", value);
        Map<String, Object> entry = Map.of("changes", List.of(change));
        Map<String, Object> payload = Map.of("entry", List.of(entry));

        return mockMvc.perform(post("/api/whatsapp/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
    }

    private int createAppointmentAndReturnId() throws Exception {
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
        payload.put("customerName", "Webhook");
        payload.put("customerSurname", "Test");
        payload.put("phone", "05551230099");
        payload.put("appointmentDate", LocalDate.of(2026, 8, 17).toString());
        payload.put("appointmentTime", "10:00");

        String response = mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("appointmentId").asInt();
    }

    private Notification onlyNotification(int appointmentId) {
        return notificationRepository.findAll().stream()
                .filter(item -> item.getAppointment() != null
                        && appointmentId == item.getAppointment().getAppointmentId())
                .findFirst().orElseThrow();
    }
}
