package glowbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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

    public record ForgotPasswordRequest(
            @NotBlank(message = "E-posta boş olamaz.") String email
    ) {
    }

    public record ResetPasswordRequest(
            @NotBlank(message = "Bağlantı bilgisi eksik.") String token,
            @NotBlank(message = "Şifre boş olamaz.")
            @Size(min = 6, message = "Şifre en az 6 karakter olmalıdır.") String newPassword
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
