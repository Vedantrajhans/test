package com.vedant.concert_platform.dto;

import com.vedant.concert_platform.entity.enums.Status;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

public class OrganizerDto {

    @Data
    public static class OrganizerRequest {
        @NotBlank
        @jakarta.validation.constraints.Email
        private String email;
        private String firstName;
        private String lastName;

        @NotBlank
        private String organizerType;

        private List<String> preferredGenres;
        private String companyName;
        private String address;
        private String city;
        private String state;
        private String notes;
    }

    @Data
    public static class OrganizerResponse {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;
        private String organizerType;
        private List<String> preferredGenres;
        private String companyName;
        private String city;
        private Status status;
        private boolean verified;
    }
}
