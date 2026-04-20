package com.vedant.concert_platform.controller;

import com.vedant.concert_platform.dto.AuthDto;
import com.vedant.concert_platform.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthDto.TokenResponse> register(@Valid @RequestBody AuthDto.RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDto.TokenResponse> login(@Valid @RequestBody AuthDto.LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    /** Fix #9: Attendee OTP login — request OTP to login without password */
    @PostMapping("/login/otp/request")
    public ResponseEntity<AuthDto.TokenResponse> requestOtpLogin(@Valid @RequestBody AuthDto.ForgotPasswordRequest req) {
        return ResponseEntity.ok(authService.requestOtpLogin(req));
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<AuthDto.TokenResponse> verifyMfa(@Valid @RequestBody AuthDto.MfaVerifyRequest req) {
        return ResponseEntity.ok(authService.verifyMfa(req));
    }

    @PostMapping("/organizer/setup-password")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<AuthDto.TokenResponse> setupPassword(
            @Valid @RequestBody AuthDto.OrganizerSetupRequest req) {
        return ResponseEntity.ok(authService.setupOrganizerPassword(req));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody AuthDto.ForgotPasswordRequest req) {
        String token = authService.forgotPassword(req);
        return ResponseEntity.ok(Map.of(
                "message", "If that email exists, an OTP has been sent.",
                "mfaToken", token != null ? token : ""
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody AuthDto.ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully. Please log in."));
    }

    @PostMapping("/mfa/enable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthDto.MfaSetupResponse> enableMfa() {
        return ResponseEntity.ok(authService.enableMfa());
    }

    // Fix #5: organizers blocked from disabling their own MFA (enforced in service layer)
    @PostMapping("/mfa/disable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> disableMfa() {
        authService.disableMfa();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-password/request-otp")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> requestChangePasswordOtp() {
        String token = authService.requestChangePasswordOtp();
        return ResponseEntity.ok(Map.of("mfaToken", token,
                "message", "OTP sent to your email."));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody AuthDto.ChangePasswordRequest req) {
        authService.changePassword(req);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }

    // Fix #27: real profile endpoints
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthDto.ProfileResponse> getProfile() {
        return ResponseEntity.ok(authService.getProfile());
    }

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthDto.ProfileResponse> updateProfile(
            @RequestBody AuthDto.UpdateProfileRequest req) {
        return ResponseEntity.ok(authService.updateProfile(req));
    }
}
