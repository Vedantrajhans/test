package com.vedant.concert_platform.dto;

import com.vedant.concert_platform.entity.enums.Role;
import com.vedant.concert_platform.entity.enums.Status;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

public class HierarchyDto {

    @Data
    public static class CreatePromoterRequest {
        @NotBlank @Email private String email;
        @NotBlank private String firstName;
        @NotBlank private String lastName;
        @NotBlank @Size(min = 8) private String password;
        public String getEmail() { return email; }
        public void setEmail(String e) { this.email = e; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String f) { this.firstName = f; }
        public String getLastName() { return lastName; }
        public void setLastName(String l) { this.lastName = l; }
        public String getPassword() { return password; }
        public void setPassword(String p) { this.password = p; }
    }

    @Data
    public static class UpdatePromoterRequest {
        @NotBlank private String firstName;
        @NotBlank private String lastName;
        private Status status;
        public String getFirstName() { return firstName; }
        public void setFirstName(String f) { this.firstName = f; }
        public String getLastName() { return lastName; }
        public void setLastName(String l) { this.lastName = l; }
        public Status getStatus() { return status; }
        public void setStatus(Status s) { this.status = s; }
    }

    @Data
    public static class PromoterResponse {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;
        private Status status;
        public Long getId() { return id; }
        public void setId(Long i) { this.id = i; }
        public String getEmail() { return email; }
        public void setEmail(String e) { this.email = e; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String f) { this.firstName = f; }
        public String getLastName() { return lastName; }
        public void setLastName(String l) { this.lastName = l; }
        public Status getStatus() { return status; }
        public void setStatus(Status s) { this.status = s; }
    }

    @Data
    public static class CreateOrganizerRequest {
        @NotBlank @Email private String email;
        @NotBlank private String firstName;
        @NotBlank private String lastName;
        private String organizerType;
        private String companyName;
        private String city;
        private String state;
        public String getEmail() { return email; }
        public void setEmail(String e) { this.email = e; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String f) { this.firstName = f; }
        public String getLastName() { return lastName; }
        public void setLastName(String l) { this.lastName = l; }
        public String getOrganizerType() { return organizerType; }
        public void setOrganizerType(String o) { this.organizerType = o; }
        public String getCompanyName() { return companyName; }
        public void setCompanyName(String c) { this.companyName = c; }
        public String getCity() { return city; }
        public void setCity(String c) { this.city = c; }
        public String getState() { return state; }
        public void setState(String s) { this.state = s; }
    }

    @Data
    public static class UpdateOrganizerRequest {
        @NotBlank private String firstName;
        @NotBlank private String lastName;
        private String organizerType;
        private String companyName;
        private String city;
        private String state;
        private Status status;
        public String getFirstName() { return firstName; }
        public void setFirstName(String f) { this.firstName = f; }
        public String getLastName() { return lastName; }
        public void setLastName(String l) { this.lastName = l; }
        public String getOrganizerType() { return organizerType; }
        public void setOrganizerType(String o) { this.organizerType = o; }
        public String getCompanyName() { return companyName; }
        public void setCompanyName(String c) { this.companyName = c; }
        public String getCity() { return city; }
        public void setCity(String c) { this.city = c; }
        public String getState() { return state; }
        public void setState(String s) { this.state = s; }
        public Status getStatus() { return status; }
        public void setStatus(Status s) { this.status = s; }
    }

    @Data
    public static class OrganizerResponse {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;
        private String organizerType;
        private String companyName;
        private String city;
        private String state;
        private Status status;
        private boolean firstLoginRequired;
        public Long getId() { return id; }
        public void setId(Long i) { this.id = i; }
        public String getEmail() { return email; }
        public void setEmail(String e) { this.email = e; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String f) { this.firstName = f; }
        public String getLastName() { return lastName; }
        public void setLastName(String l) { this.lastName = l; }
        public String getOrganizerType() { return organizerType; }
        public void setOrganizerType(String o) { this.organizerType = o; }
        public String getCompanyName() { return companyName; }
        public void setCompanyName(String c) { this.companyName = c; }
        public String getCity() { return city; }
        public void setCity(String c) { this.city = c; }
        public String getState() { return state; }
        public void setState(String s) { this.state = s; }
        public Status getStatus() { return status; }
        public void setStatus(Status s) { this.status = s; }
        public boolean isFirstLoginRequired() { return firstLoginRequired; }
        public void setFirstLoginRequired(boolean f) { this.firstLoginRequired = f; }
    }

    @Data
    public static class UserSummary {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;
        private Role role;
        private Status status;
        public Long getId() { return id; }
        public void setId(Long i) { this.id = i; }
        public String getEmail() { return email; }
        public void setEmail(String e) { this.email = e; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String f) { this.firstName = f; }
        public String getLastName() { return lastName; }
        public void setLastName(String l) { this.lastName = l; }
        public Role getRole() { return role; }
        public void setRole(Role r) { this.role = r; }
        public Status getStatus() { return status; }
        public void setStatus(Status s) { this.status = s; }
    }

    @Data
    public static class CsvImportResult {
        private int createdCount;
        private int updatedCount;
        private List<CsvRowError> rowErrors;
        public int getCreatedCount() { return createdCount; }
        public void setCreatedCount(int c) { this.createdCount = c; }
        public int getUpdatedCount() { return updatedCount; }
        public void setUpdatedCount(int u) { this.updatedCount = u; }
        public List<CsvRowError> getRowErrors() { return rowErrors; }
        public void setRowErrors(List<CsvRowError> r) { this.rowErrors = r; }
    }

    @Data
    public static class CsvRowError {
        private int row;
        private String field;
        private String message;
        public CsvRowError(int row, String field, String message) {
            this.row = row; this.field = field; this.message = message;
        }
        public int getRow() { return row; }
        public void setRow(int r) { this.row = r; }
        public String getField() { return field; }
        public void setField(String f) { this.field = f; }
        public String getMessage() { return message; }
        public void setMessage(String m) { this.message = m; }
    }
}
