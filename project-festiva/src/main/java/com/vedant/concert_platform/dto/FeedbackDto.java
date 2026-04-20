package com.vedant.concert_platform.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

public class FeedbackDto {

    @Data
    public static class CreateRequest {
        @NotNull private Long concertId;
        @NotNull @Min(1) @Max(5) private Integer rating;
        private String comment;
        @Min(1) @Max(5) private Integer soundQuality;
        @Min(1) @Max(5) private Integer venueExperience;
        @Min(1) @Max(5) private Integer artistPerformance;
        public Long getConcertId() { return concertId; }
        public void setConcertId(Long c) { this.concertId = c; }
        public Integer getRating() { return rating; }
        public void setRating(Integer r) { this.rating = r; }
        public String getComment() { return comment; }
        public void setComment(String c) { this.comment = c; }
        public Integer getSoundQuality() { return soundQuality; }
        public void setSoundQuality(Integer s) { this.soundQuality = s; }
        public Integer getVenueExperience() { return venueExperience; }
        public void setVenueExperience(Integer v) { this.venueExperience = v; }
        public Integer getArtistPerformance() { return artistPerformance; }
        public void setArtistPerformance(Integer a) { this.artistPerformance = a; }
    }

    @Data
    public static class Response {
        private Long id;
        private Long concertId;
        private Long userId;
        private Integer rating;
        private String comment;
        private Integer soundQuality;
        private Integer venueExperience;
        private Integer artistPerformance;
        public Long getId() { return id; }
        public void setId(Long i) { this.id = i; }
        public Long getConcertId() { return concertId; }
        public void setConcertId(Long c) { this.concertId = c; }
        public Long getUserId() { return userId; }
        public void setUserId(Long u) { this.userId = u; }
        public Integer getRating() { return rating; }
        public void setRating(Integer r) { this.rating = r; }
        public String getComment() { return comment; }
        public void setComment(String c) { this.comment = c; }
        public Integer getSoundQuality() { return soundQuality; }
        public void setSoundQuality(Integer s) { this.soundQuality = s; }
        public Integer getVenueExperience() { return venueExperience; }
        public void setVenueExperience(Integer v) { this.venueExperience = v; }
        public Integer getArtistPerformance() { return artistPerformance; }
        public void setArtistPerformance(Integer a) { this.artistPerformance = a; }
    }
}
