package glowbook.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * One issued password-reset link. Only the SHA-256 hash of the raw token is
 * ever persisted — the raw value exists only in the email sent to the user
 * and is never stored. {@code accountType}/{@code accountId} identify either
 * a {@link Customer} ({@code customerId} as text) or an {@link Employee}
 * (its own string id, which also covers the ADMIN role since admins are
 * Employee rows) without a hard FK to either table, since a token may
 * legitimately outlive neither.
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "account_type", nullable = false, length = 20)
    private String accountType;

    @Column(name = "account_id", nullable = false, length = 50)
    private String accountId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
