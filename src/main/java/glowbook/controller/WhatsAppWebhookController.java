package glowbook.controller;

import glowbook.service.WhatsAppNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Public endpoint Meta calls directly — never carries a GlowBook session/JWT, so it is
 * exempted from normal auth in {@code SecurityConfig} and instead authenticates the
 * caller itself:
 *
 * <ul>
 *   <li>GET: Meta's one-time subscription handshake — echoes {@code hub.challenge} back
 *       only if {@code hub.verify_token} matches {@code whatsapp.webhook-verify-token}.</li>
 *   <li>POST: delivery/read/failure status callbacks for messages this app sent. Verified
 *       via the {@code X-Hub-Signature-256} HMAC when {@code whatsapp.app-secret} is
 *       configured (skipped with a warning otherwise, e.g. local/dev setups that have not
 *       configured it yet).</li>
 * </ul>
 *
 * <p>Always responds 200 to a structurally valid POST — Meta retries/backs off a webhook
 * that doesn't acknowledge quickly, and a single malformed entry must not block the
 * others in the same payload.</p>
 */
@RestController
@RequestMapping("/api/whatsapp/webhook")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookController {

    private final WhatsAppNotificationService whatsAppNotificationService;
    private final ObjectMapper objectMapper;

    @Value("${whatsapp.webhook-verify-token:}")
    private String verifyToken;

    @Value("${whatsapp.app-secret:}")
    private String appSecret;

    @GetMapping
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {
        if ("subscribe".equals(mode) && !isBlank(verifyToken) && verifyToken.equals(token) && challenge != null) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestBody(required = false) String rawBody,
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature
    ) {
        if (rawBody == null || rawBody.isBlank()) {
            return ResponseEntity.ok().build();
        }
        if (!isBlank(appSecret)) {
            if (!signatureValid(rawBody, signature)) {
                log.warn("WhatsApp webhook signature verification failed");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } else {
            log.warn("WHATSAPP_APP_SECRET is not configured; webhook signature was not verified");
        }

        try {
            processStatuses(rawBody);
        } catch (RuntimeException exception) {
            log.warn("WhatsApp webhook payload could not be processed: {}", exception.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    @SuppressWarnings("unchecked")
    private void processStatuses(String rawBody) {
        Map<String, Object> payload = objectMapper.readValue(rawBody, Map.class);
        List<Map<String, Object>> entries = (List<Map<String, Object>>) payload.getOrDefault("entry", List.of());
        for (Map<String, Object> entry : entries) {
            List<Map<String, Object>> changes = (List<Map<String, Object>>) entry.getOrDefault("changes", List.of());
            for (Map<String, Object> change : changes) {
                Object rawValue = change.get("value");
                if (!(rawValue instanceof Map)) {
                    continue;
                }
                Map<String, Object> value = (Map<String, Object>) rawValue;
                List<Map<String, Object>> statuses =
                        (List<Map<String, Object>>) value.getOrDefault("statuses", List.of());
                for (Map<String, Object> status : statuses) {
                    Object id = status.get("id");
                    Object statusValue = status.get("status");
                    if (id != null && statusValue != null) {
                        whatsAppNotificationService.recordDeliveryStatusUpdate(id.toString(), statusValue.toString());
                    }
                }
            }
        }
    }

    private boolean signatureValid(String rawBody, String signatureHeader) {
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computed = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String computedHex = HexFormat.of().formatHex(computed);
            String provided = signatureHeader.substring("sha256=".length());
            return MessageDigest.isEqual(
                    computedHex.getBytes(StandardCharsets.UTF_8),
                    provided.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
