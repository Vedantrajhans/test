package com.vedant.concert_platform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

public class FeedbackDto {
    @Data
    public static class CreateRequest {
        @NotNull
        private Long concertId;
        @NotNull
        @Min(1)
        @Max(5)
        private Integer rating;
        private String comment;
        @Min(1) @Max(5)
        private Integer soundQuality;
        @Min(1) @Max(5)
        private Integer venueExperience;
        @Min(1) @Max(5)
        private Integer artistPerformance;
    }

    @Data
    public static class Response {
        private Long id;
        private Long concertId;
        private Integer rating;
        private String comment;
        private Integer soundQuality;
        private Integer venueExperience;
        private Integer artistPerformance;
    }
}
