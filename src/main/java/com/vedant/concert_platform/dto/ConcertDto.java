package com.vedant.concert_platform.dto;

import com.vedant.concert_platform.entity.enums.Status;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

public class ConcertDto {

    @Data
    public static class ConcertRequest {
        @NotBlank
        private String title;
        private String description;
        private String genre;

        @NotNull
        @Future
        private LocalDateTime dateTime;
        private LocalDateTime endTime;

        private Long venueId;

        @NotNull
        @Min(1)
        private Integer totalCapacity;

        private LocalDateTime ticketSaleStart;
        private LocalDateTime ticketSaleEnd;
        private String imageUrl;
        public String getTitle() { return this.title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return this.description; }
        public void setDescription(String description) { this.description = description; }
        public String getGenre() { return this.genre; }
        public void setGenre(String genre) { this.genre = genre; }
        public LocalDateTime getDateTime() { return this.dateTime; }
        public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }
        public LocalDateTime getEndTime() { return this.endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public Long getVenueId() { return this.venueId; }
        public void setVenueId(Long venueId) { this.venueId = venueId; }
        public Integer getTotalCapacity() { return this.totalCapacity; }
        public void setTotalCapacity(Integer totalCapacity) { this.totalCapacity = totalCapacity; }
        public LocalDateTime getTicketSaleStart() { return this.ticketSaleStart; }
        public void setTicketSaleStart(LocalDateTime ticketSaleStart) { this.ticketSaleStart = ticketSaleStart; }
        public LocalDateTime getTicketSaleEnd() { return this.ticketSaleEnd; }
        public void setTicketSaleEnd(LocalDateTime ticketSaleEnd) { this.ticketSaleEnd = ticketSaleEnd; }
        public String getImageUrl() { return this.imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    }

    @Data
    public static class ConcertResponse {
        private Long id;
        private String title;
        private String description;
        private String genre;
        private LocalDateTime dateTime;
        private LocalDateTime endTime;
        private Long venueId;
        private Long organizerId;
        private Integer totalCapacity;
        private Integer ticketsSold;
        private Status status;
        private LocalDateTime ticketSaleStart;
        private LocalDateTime ticketSaleEnd;
        private String imageUrl;
        public Long getId() { return this.id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return this.title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return this.description; }
        public void setDescription(String description) { this.description = description; }
        public String getGenre() { return this.genre; }
        public void setGenre(String genre) { this.genre = genre; }
        public LocalDateTime getDateTime() { return this.dateTime; }
        public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }
        public LocalDateTime getEndTime() { return this.endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public Long getVenueId() { return this.venueId; }
        public void setVenueId(Long venueId) { this.venueId = venueId; }
        public Long getOrganizerId() { return this.organizerId; }
        public void setOrganizerId(Long organizerId) { this.organizerId = organizerId; }
        public Integer getTotalCapacity() { return this.totalCapacity; }
        public void setTotalCapacity(Integer totalCapacity) { this.totalCapacity = totalCapacity; }
        public Integer getTicketsSold() { return this.ticketsSold; }
        public void setTicketsSold(Integer ticketsSold) { this.ticketsSold = ticketsSold; }
        public Status getStatus() { return this.status; }
        public void setStatus(Status status) { this.status = status; }
        public LocalDateTime getTicketSaleStart() { return this.ticketSaleStart; }
        public void setTicketSaleStart(LocalDateTime ticketSaleStart) { this.ticketSaleStart = ticketSaleStart; }
        public LocalDateTime getTicketSaleEnd() { return this.ticketSaleEnd; }
        public void setTicketSaleEnd(LocalDateTime ticketSaleEnd) { this.ticketSaleEnd = ticketSaleEnd; }
        public String getImageUrl() { return this.imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    }
}
