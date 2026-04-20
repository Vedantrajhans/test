package com.vedant.concert_platform.dto;

import com.vedant.concert_platform.entity.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthDto {

    @Data
    public static class LoginRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;
        public String getEmail() { return email; }
        public void setEmail(String e) { this.email = e; }
        public String getPassword() { return password; }
        public void setPassword(String p) { this.password = p; }
    }

    @Data
    public static class RegisterRequest {
        @NotBlank @Email
        private String email;
        @NotBlank @Size(min = 8)
        private String password;
        @NotBlank
        private String firstName;
        @NotBlank
        private String lastName;
        private String phone;
        public String getEmail() { return email; }
        public void setEmail(String e) { this.email = e; }
        public String getPassword() { return password; }
        public void setPassword(String p) { this.password = p; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String f) { this.firstName = f; }
        public String getLastName() { return lastName; }
        public void setLastName(String l) { this.lastName = l; }
        public String getPhone() { return phone; }
        public void setPhone(String p) { this.phone = p; }
    }

    @Data
    public static class MfaVerifyRequest {
        @NotBlank
        private String email;
        @NotBlank
        private String code;
        @NotBlank
        private String mfaToken;
        public String getEmail() { return email; }
        public void setEmail(String e) { this.email = e; }
        public String getCode() { return code; }
        public void setCode(String c) { this.code = c; }
        public String getMfaToken() { return mfaToken; }
        public void setMfaToken(String m) { this.mfaToken = m; }
    }

    /** Used by organizer on first-login: set new password (OTP already verified at MFA step) */
    @Data
    public static class OrganizerSetupRequest {
        @NotBlank @Size(min = 8)
        private String newPassword;
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String p) { this.newPassword = p; }
    }

    /** Request to trigger forgot-password OTP email */
    @Data
    public static class ForgotPasswordRequest {
        @NotBlank @Email
        private String email;
        public String getEmail() { return email; }
        public void setEmail(String e) { this.email = e; }
    }

    /** Reset password using OTP received by email */
    @Data
    public static class ResetPasswordRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String mfaToken;
        @NotBlank
        private String otpCode;
        @NotBlank @Size(min = 8)
        private String newPassword;
        public String getEmail() { return email; }
        public void setEmail(String e) { this.email = e; }
        public String getMfaToken() { return mfaToken; }
        public void setMfaToken(String m) { this.mfaToken = m; }
        public String getOtpCode() { return otpCode; }
        public void setOtpCode(String o) { this.otpCode = o; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String p) { this.newPassword = p; }
    }

    /** Change password from profile (OTP-verified) */
    @Data
    public static class ChangePasswordRequest {
        @NotBlank
        private String currentPassword;
        @NotBlank @Size(min = 8)
        private String newPassword;
        @NotBlank
        private String mfaToken;
        @NotBlank
        private String otpCode;
        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String c) { this.currentPassword = c; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String n) { this.newPassword = n; }
        public String getMfaToken() { return mfaToken; }
        public void setMfaToken(String m) { this.mfaToken = m; }
        public String getOtpCode() { return otpCode; }
        public void setOtpCode(String o) { this.otpCode = o; }
    }

    // Fix #27: profile update DTOs
    @Data
    public static class UpdateProfileRequest {
        private String firstName;
        private String lastName;
        private String phone;
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }

    @Data
    public static class ProfileResponse {
        private String email;
        private String firstName;
        private String lastName;
        private String phone;
        private Role role;
        private boolean mfaEnabled;
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public Role getRole() { return role; }
        public void setRole(Role role) { this.role = role; }
        public boolean isMfaEnabled() { return mfaEnabled; }
        public void setMfaEnabled(boolean m) { this.mfaEnabled = m; }
    }

    @Data
    public static class MfaSetupResponse {
        private String message;
        public String getMessage() { return message; }
        public void setMessage(String m) { this.message = m; }
    }

    @Data
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
        private String mfaToken;
        private Role role;
        private boolean mfaRequired;
        private boolean firstLoginRequired;
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String a) { this.accessToken = a; }
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String r) { this.refreshToken = r; }
        public String getMfaToken() { return mfaToken; }
        public void setMfaToken(String m) { this.mfaToken = m; }
        public Role getRole() { return role; }
        public void setRole(Role r) { this.role = r; }
        public boolean getMfaRequired() { return mfaRequired; }
        public void setMfaRequired(boolean m) { this.mfaRequired = m; }
        public boolean getFirstLoginRequired() { return firstLoginRequired; }
        public void setFirstLoginRequired(boolean f) { this.firstLoginRequired = f; }
    }
}
