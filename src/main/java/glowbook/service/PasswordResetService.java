package glowbook.service;

import glowbook.entity.Customer;
import glowbook.entity.Employee;
import glowbook.entity.PasswordResetToken;
import glowbook.exception.BusinessException;
import glowbook.repository.CustomerRepository;
import glowbook.repository.EmployeeRepository;
import glowbook.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Email-based password reset for both account tables (Customer, and Employee
 * — which also covers ADMIN, since admins are Employee rows distinguished
 * only by role). Deliberately never reveals whether a given email exists:
 * {@link #requestReset} always completes the same way regardless of what it
 * found, and the controller returns one fixed message either way.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PasswordResetService {

    private static final int TOKEN_VALID_MINUTES = 30;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailSender mailSender;

    @Value("${app.frontend-url:https://glowbook-flutter.vercel.app}")
    private String frontendUrl;

    @Transactional
    public void requestReset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        String normalized = email.trim();

        Optional<Customer> customer = customerRepository.findByEmail(normalized);
        if (customer.isPresent() && Boolean.TRUE.equals(customer.get().getActive())) {
            issueToken("CUSTOMER", customer.get().getCustomerId().toString(), normalized);
            return;
        }

        Optional<Employee> employee = employeeRepository.findByEmailAndActiveTrue(normalized);
        employee.ifPresent(value -> issueToken("EMPLOYEE", value.getEmployeeId(), normalized));
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException("Geçersiz veya süresi dolmuş bağlantı.");
        }

        PasswordResetToken token = tokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BusinessException("Geçersiz veya süresi dolmuş bağlantı."));

        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Geçersiz veya süresi dolmuş bağlantı.");
        }

        // Marked used before touching the account row, so a retried/replayed
        // request can never apply the same token twice even under a race.
        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);

        String encoded = passwordEncoder.encode(newPassword);
        if ("CUSTOMER".equals(token.getAccountType())) {
            Customer customer = customerRepository.findById(Integer.valueOf(token.getAccountId()))
                    .orElseThrow(() -> new BusinessException("Geçersiz veya süresi dolmuş bağlantı."));
            customer.setPassword(encoded);
            customerRepository.save(customer);
        } else {
            Employee employee = employeeRepository.findById(token.getAccountId())
                    .orElseThrow(() -> new BusinessException("Geçersiz veya süresi dolmuş bağlantı."));
            employee.setPassword(encoded);
            employeeRepository.save(employee);
        }
    }

    private void issueToken(String accountType, String accountId, String email) {
        String rawToken = generateRawToken();
        PasswordResetToken token = PasswordResetToken.builder()
                .accountType(accountType)
                .accountId(accountId)
                .tokenHash(hash(rawToken))
                .expiresAt(LocalDateTime.now().plusMinutes(TOKEN_VALID_MINUTES))
                .build();
        tokenRepository.save(token);

        String link = frontendUrl + "/reset-password?token=" + rawToken;
        mailSender.sendMail(
                email,
                "GlowBook şifre sıfırlama",
                "GlowBook hesabınız için şifre sıfırlama talebi aldık.\n\n"
                        + "Şifrenizi sıfırlamak için bu bağlantıya tıklayın (30 dakika geçerlidir):\n"
                        + link
                        + "\n\nBu talebi siz yapmadıysanız bu e-postayı yok sayabilirsiniz."
        );
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }
}
