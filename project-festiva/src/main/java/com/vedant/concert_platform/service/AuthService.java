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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final JavaMailSender mailSender;

    // Fix #22: note — this in-memory store doesn't survive restarts.
    // For production, replace with Redis or a DB-backed pending_mfa_tokens table.
    private final Map<String, MfaChallenge> pendingMfaTokens = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.mfa.challenge.expiration-minutes:5}")
    private long mfaChallengeExpirationMinutes;

    @Value("${app.mfa.max-attempts:5}")
    private int mfaMaxAttempts;

    @Value("${app.mail.from:no-reply@concert.local}")
    private String mailFrom;

    // ── REGISTER (attendees) ──────────────────────────────────────────────────
    public AuthDto.TokenResponse register(AuthDto.RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail()))
            throw new ConflictException("Email already in use");

        User user = new User();
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());
        user.setPhone(req.getPhone());
        user.setRole(Role.ATTENDEE);
        user.setFirstLogin(false);
        // Fix #9: attendees get MFA enabled by default so they can use OTP login
        user.setMfaEnabled(false); // opt-in: they can enable via profile
        userRepository.save(user);
        return buildTokenResponse(user);
    }

    /** Fix #9: Attendee can request OTP to login without password */
    public AuthDto.TokenResponse requestOtpLogin(AuthDto.ForgotPasswordRequest req) {
        User user = userRepository.findByEmail(req.getEmail()).orElse(null);
        if (user == null || user.getRole() != Role.ATTENDEE) {
            // Return fake success to avoid email enumeration
            AuthDto.TokenResponse res = new AuthDto.TokenResponse();
            res.setMfaRequired(true);
            res.setMfaToken("invalid");
            res.setRole(Role.ATTENDEE);
            return res;
        }
        removeExpiredChallenges();
        String otp = generateOtp();
        String mfaToken = UUID.randomUUID().toString();
        pendingMfaTokens.put(mfaToken, new MfaChallenge(
                user.getEmail(), hashOtp(otp),
                LocalDateTime.now().plusMinutes(mfaChallengeExpirationMinutes),
                new AtomicInteger(0)));
        sendOtpEmail(user.getEmail(), otp, "Your Festiva login OTP",
                "Use the OTP below to sign in to Festiva without your password.");
        AuthDto.TokenResponse res = new AuthDto.TokenResponse();
        res.setMfaRequired(true);
        res.setMfaToken(mfaToken);
        res.setRole(user.getRole());
        return res;
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────────
    public AuthDto.TokenResponse login(AuthDto.LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));

        UserDetails ud = (UserDetails) auth.getPrincipal();
        User user = userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Auto-enable MFA for organizer on first login
        if (user.getRole() == Role.ORGANIZER && user.isFirstLogin() && !user.isMfaEnabled()) {
            user.setMfaEnabled(true);
            userRepository.save(user);
        }

        if (user.isMfaEnabled()) {
            removeExpiredChallenges();
            String otp      = generateOtp();
            String mfaToken = UUID.randomUUID().toString();
            pendingMfaTokens.put(mfaToken, new MfaChallenge(
                    user.getEmail(), hashOtp(otp),
                    LocalDateTime.now().plusMinutes(mfaChallengeExpirationMinutes),
                    new AtomicInteger(0)));

            boolean firstLogin = user.isFirstLogin();
            sendOtpEmail(user.getEmail(), otp,
                    firstLogin ? "Set your Festiva password — OTP" : "Your Festiva login OTP",
                    firstLogin
                        ? "You are logging in for the first time. Verify your identity with the OTP below before setting your new password."
                        : "Use the OTP below to complete your login.");

            AuthDto.TokenResponse res = new AuthDto.TokenResponse();
            res.setMfaRequired(true);
            res.setFirstLoginRequired(firstLogin);
            res.setMfaToken(mfaToken);
            res.setRole(user.getRole());
            return res;
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        return buildTokenResponse(user);
    }

    // ── VERIFY MFA (normal login) ─────────────────────────────────────────────
    public AuthDto.TokenResponse verifyMfa(AuthDto.MfaVerifyRequest req) {
        removeExpiredChallenges();
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isMfaEnabled())
            throw new BadRequestException("MFA is not enabled for this user");

        MfaChallenge ch = pendingMfaTokens.get(req.getMfaToken());
        if (ch == null || ch.expiresAt().isBefore(LocalDateTime.now()) || !ch.email().equals(user.getEmail()))
            throw new BadRequestException("OTP challenge is invalid or expired");

        // Fix #4: brute-force protection — lock after max attempts
        if (ch.attempts().get() >= mfaMaxAttempts) {
            pendingMfaTokens.remove(req.getMfaToken());
            throw new BadRequestException("Too many incorrect attempts. Please request a new OTP.");
        }
        if (!isOtpMatch(ch.codeHash(), req.getCode())) {
            ch.attempts().incrementAndGet();
            int remaining = mfaMaxAttempts - ch.attempts().get();
            throw new BadRequestException("Invalid OTP code. " + remaining + " attempt(s) remaining.");
        }

        pendingMfaTokens.remove(req.getMfaToken());
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        AuthDto.TokenResponse res = new AuthDto.TokenResponse();
        res.setAccessToken(jwtUtil.generateToken(toUserDetails(user)));
        res.setRole(user.getRole());
        res.setMfaRequired(false);
        res.setFirstLoginRequired(user.isFirstLogin());
        return res;
    }

    // ── ORGANIZER FIRST-LOGIN: SET NEW PASSWORD ───────────────────────────────
    public AuthDto.TokenResponse setupOrganizerPassword(AuthDto.OrganizerSetupRequest req) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isFirstLogin() || user.getRole() != Role.ORGANIZER)
            throw new BadRequestException("Account already set up or not an organizer");

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setFirstLogin(false);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return buildTokenResponse(user);
    }

    // ── CHANGE PASSWORD ───────────────────────────────────────────────────────
    public String requestChangePasswordOtp() {
        User user = currentUser();
        removeExpiredChallenges();
        String otp      = generateOtp();
        String mfaToken = UUID.randomUUID().toString();
        pendingMfaTokens.put(mfaToken, new MfaChallenge(
                user.getEmail(), hashOtp(otp),
                LocalDateTime.now().plusMinutes(mfaChallengeExpirationMinutes),
                new AtomicInteger(0)));
        sendOtpEmail(user.getEmail(), otp, "Festiva — Change Password OTP",
                "You requested to change your password. Use the OTP below to confirm.");
        return mfaToken;
    }

    public void changePassword(AuthDto.ChangePasswordRequest req) {
        User user = currentUser();
        removeExpiredChallenges();

        MfaChallenge ch = pendingMfaTokens.get(req.getMfaToken());
        if (ch == null || ch.expiresAt().isBefore(LocalDateTime.now()) || !ch.email().equals(user.getEmail()))
            throw new BadRequestException("OTP is invalid or expired — request a new one");

        // Fix #4: brute-force protection on password change OTP too
        if (ch.attempts().get() >= mfaMaxAttempts) {
            pendingMfaTokens.remove(req.getMfaToken());
            throw new BadRequestException("Too many incorrect attempts. Please request a new OTP.");
        }
        if (!isOtpMatch(ch.codeHash(), req.getOtpCode())) {
            ch.attempts().incrementAndGet();
            throw new BadRequestException("Incorrect OTP code.");
        }
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword()))
            throw new BadRequestException("Current password is incorrect");

        pendingMfaTokens.remove(req.getMfaToken());
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }

    // ── FORGOT PASSWORD ───────────────────────────────────────────────────────
    public String forgotPassword(AuthDto.ForgotPasswordRequest req) {
        User user = userRepository.findByEmail(req.getEmail()).orElse(null);
        if (user == null) return null; // silent — no enumeration

        removeExpiredChallenges();
        String otp      = generateOtp();
        String mfaToken = UUID.randomUUID().toString();
        pendingMfaTokens.put(mfaToken, new MfaChallenge(
                user.getEmail(), hashOtp(otp),
                LocalDateTime.now().plusMinutes(mfaChallengeExpirationMinutes),
                new AtomicInteger(0)));

        sendOtpEmail(user.getEmail(), otp,
                "Reset your Festiva password",
                "Use the OTP below to reset your password. It expires in "
                        + mfaChallengeExpirationMinutes + " minutes.");

        log.info("Forgot-password OTP issued for {} (token={})", user.getEmail(), mfaToken);
        return mfaToken;
    }

    public void resetPassword(AuthDto.ResetPasswordRequest req) {
        removeExpiredChallenges();
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        MfaChallenge ch = pendingMfaTokens.get(req.getMfaToken());
        if (ch == null || ch.expiresAt().isBefore(LocalDateTime.now()) || !ch.email().equals(user.getEmail()))
            throw new BadRequestException("OTP is invalid or expired — request a new one");

        // Fix #4: brute-force on reset too
        if (ch.attempts().get() >= mfaMaxAttempts) {
            pendingMfaTokens.remove(req.getMfaToken());
            throw new BadRequestException("Too many incorrect attempts. Please request a new OTP.");
        }
        if (!isOtpMatch(ch.codeHash(), req.getOtpCode())) {
            ch.attempts().incrementAndGet();
            throw new BadRequestException("Incorrect OTP code.");
        }

        pendingMfaTokens.remove(req.getMfaToken());
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }

    // ── PROFILE ───────────────────────────────────────────────────────────────
    // Fix #27: real profile get/update
    public AuthDto.ProfileResponse getProfile() {
        User user = currentUser();
        AuthDto.ProfileResponse r = new AuthDto.ProfileResponse();
        r.setEmail(user.getEmail());
        r.setFirstName(user.getFirstName());
        r.setLastName(user.getLastName());
        r.setPhone(user.getPhone());
        r.setRole(user.getRole());
        r.setMfaEnabled(user.isMfaEnabled());
        return r;
    }

    public AuthDto.ProfileResponse updateProfile(AuthDto.UpdateProfileRequest req) {
        User user = currentUser();
        if (req.getFirstName() != null) user.setFirstName(req.getFirstName().trim());
        if (req.getLastName() != null) user.setLastName(req.getLastName().trim());
        if (req.getPhone() != null) user.setPhone(req.getPhone().trim());
        userRepository.save(user);
        return getProfile();
    }

    // ── MFA ENABLE / DISABLE ──────────────────────────────────────────────────
    public AuthDto.MfaSetupResponse enableMfa() {
        User user = currentUser();
        if (user.isMfaEnabled()) throw new BadRequestException("MFA already enabled");
        user.setMfaEnabled(true);
        userRepository.save(user);
        AuthDto.MfaSetupResponse r = new AuthDto.MfaSetupResponse();
        r.setMessage("MFA enabled. OTP codes will be sent to your email.");
        return r;
    }

    public void disableMfa() {
        User user = currentUser();
        // Fix #5: organizers must not be able to disable their own MFA
        if (user.getRole() == Role.ORGANIZER) {
            throw new BadRequestException("Organizers cannot disable MFA. MFA is required for all organizer actions.");
        }
        if (!user.isMfaEnabled()) throw new BadRequestException("MFA not enabled");
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────────
    private AuthDto.TokenResponse buildTokenResponse(User user) {
        AuthDto.TokenResponse r = new AuthDto.TokenResponse();
        r.setAccessToken(jwtUtil.generateToken(toUserDetails(user)));
        r.setRole(user.getRole());
        r.setMfaRequired(false);
        r.setFirstLoginRequired(user.isFirstLogin());
        return r;
    }

    private UserDetails toUserDetails(User user) {
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(), user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }

    private User currentUser() {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(name)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private void sendOtpEmail(String to, String code, String subject, String intro) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(mailFrom);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText("Hi,\n\n" + intro + "\n\n"
                    + "OTP Code: " + code + "\n\n"
                    + "This code expires in " + mfaChallengeExpirationMinutes + " minutes.\n\n"
                    + "— Festiva Team");
            mailSender.send(msg);
        } catch (Exception ex) {
            log.warn("Failed to send OTP email to {}: {}", to, ex.getMessage());
            throw new BadRequestException("Could not send OTP email. Please contact support.");
        }
    }

    private String hashOtp(String code) {
        try {
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            byte[] h = d.digest(code.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(h.length * 2);
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private boolean isOtpMatch(String expectedHash, String provided) {
        return MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.UTF_8),
                hashOtp(provided).getBytes(StandardCharsets.UTF_8));
    }

    private void removeExpiredChallenges() {
        LocalDateTime now = LocalDateTime.now();
        pendingMfaTokens.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
    }

    record MfaChallenge(String email, String codeHash, LocalDateTime expiresAt, AtomicInteger attempts) {}
}
