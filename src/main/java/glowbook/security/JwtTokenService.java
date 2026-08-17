package glowbook.security;

import glowbook.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JwtTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${app.jwt.secret:glowbook-local-secret-change-me}")
    private String secret;

    @Value("${app.jwt.expiration-seconds:86400}")
    private long expirationSeconds;

    public String generateToken(String subject, UserRole role) {
        long now = Instant.now().getEpochSecond();
        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = base64Url("{"
                + "\"sub\":\"" + escape(subject) + "\","
                + "\"role\":\"" + role.name() + "\","
                + "\"iat\":" + now + ","
                + "\"exp\":" + (now + expirationSeconds)
                + "}");
        String signature = sign(header + "." + payload);
        return header + "." + payload + "." + signature;
    }

    public boolean isValid(String token) {
        try {
            Map<String, String> claims = parseClaims(token);
            long exp = Long.parseLong(claims.get("exp"));
            return exp > Instant.now().getEpochSecond();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public String getSubject(String token) {
        return parseClaims(token).get("sub");
    }

    public UserRole getRole(String token) {
        return UserRole.valueOf(parseClaims(token).get("role"));
    }

    private Map<String, String> parseClaims(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new BusinessException("Oturum süren dolmuş olabilir. Lütfen tekrar giriş yap.");
        }

        String expectedSignature = sign(parts[0] + "." + parts[1]);
        if (!constantTimeEquals(expectedSignature, parts[2])) {
            throw new BusinessException("Oturum süren dolmuş olabilir. Lütfen tekrar giriş yap.");
        }

        String json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        return parseFlatJson(json);
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new BusinessException("Oturum oluşturulamadı. Lütfen daha sonra tekrar dene.");
        }
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private Map<String, String> parseFlatJson(String json) {
        Map<String, String> claims = new LinkedHashMap<>();
        String content = json.substring(1, json.length() - 1);
        for (String pair : content.split(",")) {
            String[] keyValue = pair.split(":", 2);
            String key = keyValue[0].trim().replace("\"", "");
            String value = keyValue[1].trim().replace("\"", "");
            claims.put(key, value);
        }
        return claims;
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private boolean constantTimeEquals(String first, String second) {
        return MessageDigestUtil.equals(first.getBytes(StandardCharsets.UTF_8), second.getBytes(StandardCharsets.UTF_8));
    }
}
