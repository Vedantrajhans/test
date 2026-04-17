package com.vedant.concert_platform.service;

import com.vedant.concert_platform.dto.AuthDto;
import com.vedant.concert_platform.entity.User;
import com.vedant.concert_platform.entity.enums.Role;
import com.vedant.concert_platform.exception.BadRequestException;
import com.vedant.concert_platform.exception.ConflictException;
import com.vedant.concert_platform.exception.ResourceNotFoundException;
import com.vedant.concert_platform.repository.UserRepository;
import com.vedant.concert_platform.security.JwtUtil;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.secret.SecretGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final SecretGenerator secretGenerator;
    private final CodeVerifier codeVerifier;
    private final Map<String, String> pendingMfaTokens = new ConcurrentHashMap<>();

    public AuthDto.TokenResponse register(AuthDto.RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already in use");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setRole(Role.ATTENDEE);
        user.setFirstLogin(false);

        userRepository.save(user);

        return generateTokenResponse(user);
    }

    public AuthDto.TokenResponse login(AuthDto.LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.isMfaEnabled()) {
            AuthDto.TokenResponse response = new AuthDto.TokenResponse();
            response.setMfaRequired(true);
            response.setFirstLoginRequired(user.isFirstLogin());
            String mfaToken = UUID.randomUUID().toString();
            pendingMfaTokens.put(user.getEmail(), mfaToken);
            response.setMfaToken(mfaToken);
            return response;
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return generateTokenResponse(user);
    }

    public AuthDto.TokenResponse verifyMfa(AuthDto.MfaVerifyRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isMfaEnabled()) {
            throw new BadRequestException("MFA is not enabled for this user");
        }

        boolean isValid = codeVerifier.isValidCode(user.getMfaSecret(), request.getCode());
        if (!isValid) {
            throw new BadRequestException("Invalid MFA code");
        }
        String pendingToken = pendingMfaTokens.get(user.getEmail());
        if (pendingToken == null || !pendingToken.equals(request.getMfaToken())) {
            throw new BadRequestException("MFA challenge is invalid or expired");
        }
        pendingMfaTokens.remove(user.getEmail());

        // Auth success
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        AuthDto.TokenResponse response = new AuthDto.TokenResponse();
        response.setAccessToken(jwtUtil.generateToken(userDetails));
        response.setRole(user.getRole());
        response.setMfaRequired(false);
        response.setFirstLoginRequired(user.isFirstLogin());
        return response;
    }

    public AuthDto.TokenResponse setupOrganizerPassword(AuthDto.OrganizerSetupRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isFirstLogin() || user.getRole() != Role.ORGANIZER) {
            throw new BadRequestException("Invalid operation or account already setup");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setFirstLogin(false);
        user.setLastLoginAt(LocalDateTime.now());
        
        userRepository.save(user);

        return generateTokenResponse(user);
    }

    public String generateMfaSecret(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String secret = secretGenerator.generate();
        user.setMfaSecret(secret);
        user.setMfaEnabled(true);
        userRepository.save(user);
        return secret;
    }

    public AuthDto.MfaSetupResponse enableMfa() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.isMfaEnabled()) {
            throw new BadRequestException("MFA is already enabled for this account");
        }

        String secret = secretGenerator.generate();
        user.setMfaSecret(secret);
        user.setMfaEnabled(true);
        userRepository.save(user);

        String otpauthUri = "otpauth://totp/ConcertPlatform:" + user.getEmail()
                + "?secret=" + secret + "&issuer=ConcertPlatform";

        AuthDto.MfaSetupResponse response = new AuthDto.MfaSetupResponse();
        response.setSecret(secret);
        response.setOtpauthUri(otpauthUri);
        response.setQrCodeUrl("https://quickchart.io/qr?text=" + URLEncoder.encode(otpauthUri, StandardCharsets.UTF_8));
        return response;
    }

    public void disableMfa() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isMfaEnabled()) {
            throw new BadRequestException("MFA is not enabled for this account");
        }

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);
    }

    private AuthDto.TokenResponse generateTokenResponse(User user) {
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        AuthDto.TokenResponse response = new AuthDto.TokenResponse();
        response.setAccessToken(jwtUtil.generateToken(userDetails));
        response.setRole(user.getRole());
        response.setMfaRequired(false);
        response.setFirstLoginRequired(user.isFirstLogin());
        return response;
    }
}
