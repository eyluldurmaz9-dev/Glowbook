package glowbook.dto;

import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password,
            @NotBlank String role
    ) {
    }

    public record RegisterCustomerRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank String phone,
            @NotBlank String password,
            String email
    ) {
    }

    public record RefreshRequest(
            @NotBlank String refreshToken
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
