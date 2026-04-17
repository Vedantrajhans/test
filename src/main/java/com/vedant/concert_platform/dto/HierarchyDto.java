package com.vedant.concert_platform.dto;

import com.vedant.concert_platform.entity.enums.Status;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class HierarchyDto {

    @Data
    public static class CreatePromoterRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String firstName;
        @NotBlank
        private String lastName;
        @NotBlank
        @Size(min = 8)
        private String password;
    }

    @Data
    public static class PromoterResponse {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;
        private Status status;
    }

    @Data
    public static class CreateOrganizerRequest {
        @NotBlank @Email
        private String email;
        private String firstName;
        private String lastName;
        @NotBlank
        private String organizerType;
        private String companyName;
        private String city;
        private String state;
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
    }

    @Data
    public static class CsvImportResult {
        private int createdCount;
        private int updatedCount;
    }
}
