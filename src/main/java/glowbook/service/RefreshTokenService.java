package glowbook.service;

import glowbook.entity.RefreshToken;
import glowbook.exception.BusinessException;
import glowbook.repository.RefreshTokenRepository;
import glowbook.security.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration-days:30}")
    private long refreshExpirationDays;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RefreshToken create(String subject, UserRole role) {
        String token = UUID.randomUUID().toString() + "." + UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(refreshExpirationDays, ChronoUnit.DAYS);
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .subject(subject)
                .role(role.name())
                .expiresAt(expiresAt)
                .revoked(false)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public void revoke(RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    public void validate(RefreshToken token) {
        if (token == null) throw new BusinessException("Oturum süren dolmuş olabilir. Lütfen tekrar giriş yap.");
        if (Boolean.TRUE.equals(token.getRevoked())) throw new BusinessException("Oturum süren dolmuş olabilir. Lütfen tekrar giriş yap.");
        if (token.getExpiresAt().isBefore(Instant.now())) throw new BusinessException("Oturum süren dolmuş olabilir. Lütfen tekrar giriş yap.");
    }
}
