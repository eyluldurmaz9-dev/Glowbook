package glowbook.dto;

import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "Kullanıcı adı boş olamaz.") String username,
            @NotBlank(message = "Şifre boş olamaz.") String password,
            @NotBlank(message = "Rol seçilmelidir.") String role
    ) {
    }

    public record RegisterCustomerRequest(
            @NotBlank(message = "Ad boş olamaz.") String firstName,
            @NotBlank(message = "Soyad boş olamaz.") String lastName,
            @NotBlank(message = "Telefon numarası boş olamaz.") String phone,
            @NotBlank(message = "Şifre boş olamaz.") String password,
            String email
    ) {
    }

    public record RefreshRequest(
            @NotBlank(message = "Oturum bilgisi eksik.") String refreshToken
    ) {
    }

    public record AuthResponse(
            String token,
            String refreshToken,
            String tokenType,
            String role,
            Integer customerId,
            String employeeId,
            String fullName
    ) {
    }
}
