package com.vedant.concert_platform.dto;

import com.vedant.concert_platform.entity.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthDto {

    @Data
    public static class LoginRequest {
        @NotBlank
        @Email
        private String email;

        @NotBlank
        private String password;
        public String getEmail() { return this.email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return this.password; }
        public void setPassword(String password) { this.password = password; }
    }

    @Data
    public static class RegisterRequest {
        @NotBlank
        @Email
        private String email;

        @NotBlank
        @Size(min = 8)
        private String password;

        @NotBlank
        private String firstName;

        @NotBlank
        private String lastName;

        private String phone;
        public String getEmail() { return this.email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return this.password; }
        public void setPassword(String password) { this.password = password; }
        public String getFirstName() { return this.firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return this.lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getPhone() { return this.phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }

    @Data
    public static class MfaVerifyRequest {
        @NotBlank
        private String email;
        
        @NotBlank
        private String code;
        @NotBlank
        private String mfaToken;
        public String getEmail() { return this.email; }
        public void setEmail(String email) { this.email = email; }
        public String getCode() { return this.code; }
        public void setCode(String code) { this.code = code; }
        public String getMfaToken() { return this.mfaToken; }
        public void setMfaToken(String mfaToken) { this.mfaToken = mfaToken; }
    }

    @Data
    public static class OrganizerSetupRequest {
        @NotBlank
        @Size(min = 8)
        private String newPassword;
        public String getNewPassword() { return this.newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    @Data
    public static class MfaSetupResponse {
        private String message;
        public String getMessage() { return this.message; }
        public void setMessage(String message) { this.message = message; }
    }

    @Data
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
        private String mfaToken;
        private Role role;
        private boolean mfaRequired;
        private boolean firstLoginRequired;
        public String getAccessToken() { return this.accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public String getRefreshToken() { return this.refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
        public String getMfaToken() { return this.mfaToken; }
        public void setMfaToken(String mfaToken) { this.mfaToken = mfaToken; }
        public Role getRole() { return this.role; }
        public void setRole(Role role) { this.role = role; }
        public boolean getMfaRequired() { return this.mfaRequired; }
        public void setMfaRequired(boolean mfaRequired) { this.mfaRequired = mfaRequired; }
        public boolean getFirstLoginRequired() { return this.firstLoginRequired; }
        public void setFirstLoginRequired(boolean firstLoginRequired) { this.firstLoginRequired = firstLoginRequired; }
    }
}
