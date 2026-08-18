package glowbook.controller;

import glowbook.dto.ApiResponse;
import glowbook.dto.AuthDtos;
import glowbook.service.AuthService;
import glowbook.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ApiResponse<AuthDtos.AuthResponse> register(@Valid @RequestBody AuthDtos.RegisterCustomerRequest request) {
        return ApiResponse.success("Customer registered", authService.registerCustomer(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return ApiResponse.success("Login successful", authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthDtos.AuthResponse> refresh(@Valid @RequestBody AuthDtos.RefreshRequest request) {
        return ApiResponse.success("Token refreshed", authService.refresh(request.refreshToken()));
    }

    /** Always answers the same way regardless of whether the email is registered — no user enumeration. */
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody AuthDtos.ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        return ApiResponse.success(
                "Eğer bu e-posta adresi sistemde kayıtlıysa şifre sıfırlama bağlantısı gönderildi.", null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody AuthDtos.ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ApiResponse.success("Şifreniz güncellendi.", null);
    }
}
