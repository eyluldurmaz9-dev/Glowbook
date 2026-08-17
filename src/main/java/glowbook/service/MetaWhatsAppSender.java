package glowbook.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Real WhatsApp Business Platform / Cloud API provider. Sends a business-initiated
 * template message via the official Meta Graph API endpoint — no browser automation,
 * no unofficial/session-based client. Active only when {@code app.whatsapp.provider=meta}.
 *
 * <p>Request/response bodies are handled as plain {@link Map}s rather than typed
 * DTOs/tree nodes so this class has no dependency on which exact Jackson tree/annotation
 * API is on the classpath — only {@link ObjectMapper#writeValueAsString} and {@link
 * ObjectMapper#readValue} are used, which are stable across Jackson generations.</p>
 */
@Service
@ConditionalOnProperty(name = "app.whatsapp.provider", havingValue = "meta")
public class MetaWhatsAppSender implements WhatsAppSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetaWhatsAppSender.class);

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;

    @Value("${whatsapp.access-token:}")
    private String accessToken;

    @Value("${whatsapp.phone-number-id:}")
    private String phoneNumberId;

    @Value("${whatsapp.graph-api-version:v21.0}")
    private String graphApiVersion;

    public MetaWhatsAppSender(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public WhatsAppSendResult sendTemplate(String phone, String templateName, String languageCode,
                                            List<String> bodyParameters) {
        if (isBlank(accessToken) || isBlank(phoneNumberId)) {
            return WhatsAppSendResult.failed("WhatsApp Cloud API credentials are missing");
        }
        if (isBlank(phone)) {
            return WhatsAppSendResult.failed("WhatsApp phone number is missing");
        }

        try {
            String requestBody = objectMapper.writeValueAsString(buildPayload(phone, templateName, languageCode, bodyParameters));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://graph.facebook.com/" + graphApiVersion + "/" + phoneNumberId + "/messages"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOGGER.warn("WhatsApp Cloud API request failed with status {}", response.statusCode());
                return WhatsAppSendResult.failed("WhatsApp Cloud API request failed with status " + response.statusCode());
            }
            return WhatsAppSendResult.accepted(extractMessageId(response.body()));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return WhatsAppSendResult.failed("WhatsApp sending was interrupted");
        } catch (Exception exception) {
            LOGGER.warn("WhatsApp message could not be sent: {}", exception.getMessage());
            return WhatsAppSendResult.failed("WhatsApp message could not be sent");
        }
    }

    private Map<String, Object> buildPayload(String phone, String templateName, String languageCode,
                                              List<String> bodyParameters) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("name", templateName);
        template.put("language", Map.of("code", languageCode));
        if (!bodyParameters.isEmpty()) {
            List<Map<String, String>> parameters = bodyParameters.stream()
                    .map(value -> Map.of("type", "text", "text", value))
                    .toList();
            template.put("components", List.of(Map.of("type", "body", "parameters", parameters)));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", stripLeadingPlus(phone));
        payload.put("type", "template");
        payload.put("template", template);
        return payload;
    }

    @SuppressWarnings("unchecked")
    private String extractMessageId(String responseBody) {
        Map<String, Object> parsed = objectMapper.readValue(responseBody, Map.class);
        Object messages = parsed.get("messages");
        if (messages instanceof List<?> list && !list.isEmpty() && list.getFirst() instanceof Map<?, ?> first) {
            Object id = first.get("id");
            return id == null ? null : id.toString();
        }
        return null;
    }

    /** The Cloud API expects the recipient without the leading "+". */
    private String stripLeadingPlus(String phone) {
        return phone.startsWith("+") ? phone.substring(1) : phone;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
