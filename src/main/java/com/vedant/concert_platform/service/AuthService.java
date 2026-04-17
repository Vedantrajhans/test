package com.vedant.concert_platform.service;

import com.vedant.concert_platform.dto.AuthDto;
import com.vedant.concert_platform.entity.User;
import com.vedant.concert_platform.entity.enums.Role;
import com.vedant.concert_platform.exception.BadRequestException;
import com.vedant.concert_platform.exception.ConflictException;
import com.vedant.concert_platform.exception.ResourceNotFoundException;
import com.vedant.concert_platform.repository.UserRepository;
import com.vedant.concert_platform.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final JavaMailSender mailSender;
    private final Map<String, MfaChallenge> pendingMfaTokens = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.mfa.challenge.expiration-minutes:5}")
    private long mfaChallengeExpirationMinutes;

    @Value("${app.mail.from:no-reply@concert.local}")
    private String mailFrom;

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
            removeExpiredChallenges();
            AuthDto.TokenResponse response = new AuthDto.TokenResponse();
            response.setMfaRequired(true);
            response.setFirstLoginRequired(user.isFirstLogin());
            String mfaToken = UUID.randomUUID().toString();
            String otpCode = generateOtp();
            MfaChallenge challenge = new MfaChallenge(
                    user.getEmail(),
                    hashOtp(otpCode),
                    LocalDateTime.now().plusMinutes(mfaChallengeExpirationMinutes)
            );
            pendingMfaTokens.put(mfaToken, challenge);
            try {
                sendMfaOtpEmail(user.getEmail(), otpCode);
            } catch (BadRequestException ex) {
                pendingMfaTokens.remove(mfaToken);
                throw ex;
            }
            response.setMfaToken(mfaToken);
            return response;
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return generateTokenResponse(user);
    }

    public AuthDto.TokenResponse verifyMfa(AuthDto.MfaVerifyRequest request) {
        removeExpiredChallenges();
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isMfaEnabled()) {
            throw new BadRequestException("MFA is not enabled for this user");
        }

        MfaChallenge challenge = pendingMfaTokens.get(request.getMfaToken());
        if (challenge == null || challenge.expiresAt().isBefore(LocalDateTime.now()) || !challenge.email().equals(user.getEmail())) {
            throw new BadRequestException("MFA challenge is invalid or expired");
        }
        if (!isOtpMatch(challenge.codeHash(), request.getCode())) {
            throw new BadRequestException("Invalid MFA code");
        }
        pendingMfaTokens.remove(request.getMfaToken());

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

    public AuthDto.MfaSetupResponse enableMfa() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.isMfaEnabled()) {
            throw new BadRequestException("MFA is already enabled for this account");
        }

        user.setMfaEnabled(true);
        userRepository.save(user);

        AuthDto.MfaSetupResponse response = new AuthDto.MfaSetupResponse();
        response.setMessage("MFA enabled successfully. OTP codes will be sent to your email.");
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

    private String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private void sendMfaOtpEmail(String to, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(to);
            message.setSubject("Your Concert Platform OTP");
            message.setText("Your OTP is " + code + ". It will expire in " + mfaChallengeExpirationMinutes + " minutes.");
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Failed to send MFA OTP email to {}", to, ex);
            throw new BadRequestException("Failed to send MFA code email");
        }
    }

    private boolean isOtpMatch(String expectedCodeHash, String providedCode) {
        return MessageDigest.isEqual(
                expectedCodeHash.getBytes(StandardCharsets.UTF_8),
                hashOtp(providedCode).getBytes(StandardCharsets.UTF_8)
        );
    }

    private String hashOtp(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private void removeExpiredChallenges() {
        LocalDateTime now = LocalDateTime.now();
        pendingMfaTokens.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private record MfaChallenge(String email, String codeHash, LocalDateTime expiresAt) { }
}
