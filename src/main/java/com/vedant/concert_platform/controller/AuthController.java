package com.vedant.concert_platform.controller;

import com.vedant.concert_platform.dto.AuthDto;
import com.vedant.concert_platform.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthDto.TokenResponse> register(@Valid @RequestBody AuthDto.RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDto.TokenResponse> login(@Valid @RequestBody AuthDto.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<AuthDto.TokenResponse> verifyMfa(@Valid @RequestBody AuthDto.MfaVerifyRequest request) {
        return ResponseEntity.ok(authService.verifyMfa(request));
    }

    @PostMapping("/organizer/setup-password")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<AuthDto.TokenResponse> setupPassword(@Valid @RequestBody AuthDto.OrganizerSetupRequest request) {
        return ResponseEntity.ok(authService.setupOrganizerPassword(request));
    }

    @PostMapping("/mfa/enable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthDto.MfaSetupResponse> enableMfa() {
        return ResponseEntity.ok(authService.enableMfa());
    }

    @PostMapping("/mfa/disable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> disableMfa() {
        authService.disableMfa();
        return ResponseEntity.ok().build();
    }
}
