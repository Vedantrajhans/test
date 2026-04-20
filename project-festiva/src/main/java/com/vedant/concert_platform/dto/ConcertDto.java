package com.vedant.concert_platform.dto;

import com.vedant.concert_platform.entity.enums.Status;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ConcertDto {

    @Data
    public static class ConcertRequest {
        @NotBlank
        private String title;
        private String description;
        private String genre;

        @NotNull
        private LocalDateTime dateTime;
        private LocalDateTime endTime;

        private Long venueId;
        // For inline venue creation
        private String venueAddress;
        private String venueCity;

        @NotNull
        @Min(1)
        private Integer totalCapacity;

        private LocalDateTime ticketSaleStart;
        private LocalDateTime ticketSaleEnd;
        private String imageUrl;
        private List<TicketTypeRequest> ticketTypes;

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
        public String getVenueAddress() { return this.venueAddress; }
        public void setVenueAddress(String venueAddress) { this.venueAddress = venueAddress; }
        public String getVenueCity() { return this.venueCity; }
        public void setVenueCity(String venueCity) { this.venueCity = venueCity; }
        public Integer getTotalCapacity() { return this.totalCapacity; }
        public void setTotalCapacity(Integer totalCapacity) { this.totalCapacity = totalCapacity; }
        public LocalDateTime getTicketSaleStart() { return this.ticketSaleStart; }
        public void setTicketSaleStart(LocalDateTime ticketSaleStart) { this.ticketSaleStart = ticketSaleStart; }
        public LocalDateTime getTicketSaleEnd() { return this.ticketSaleEnd; }
        public void setTicketSaleEnd(LocalDateTime ticketSaleEnd) { this.ticketSaleEnd = ticketSaleEnd; }
        public String getImageUrl() { return this.imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public List<TicketTypeRequest> getTicketTypes() { return this.ticketTypes; }
        public void setTicketTypes(List<TicketTypeRequest> ticketTypes) { this.ticketTypes = ticketTypes; }
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
        private String venueAddress;
        private String venueCity;
        private Long organizerId;
        private Integer totalCapacity;
        private Integer ticketsSold;
        private Status status;
        private LocalDateTime ticketSaleStart;
        private LocalDateTime ticketSaleEnd;
        private String imageUrl;
        private List<TicketTypeResponse> ticketTypes;

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
        public String getVenueAddress() { return this.venueAddress; }
        public void setVenueAddress(String venueAddress) { this.venueAddress = venueAddress; }
        public String getVenueCity() { return this.venueCity; }
        public void setVenueCity(String venueCity) { this.venueCity = venueCity; }
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
        public List<TicketTypeResponse> getTicketTypes() { return this.ticketTypes; }
        public void setTicketTypes(List<TicketTypeResponse> ticketTypes) { this.ticketTypes = ticketTypes; }
    }

    @Data
    public static class TicketTypeRequest {
        @NotBlank
        private String name;

        @NotNull
        @Min(1)
        private BigDecimal price;

        @NotNull
        @Min(1)
        private Integer quantity;

        public String getName() { return name; }
        public void setName(String n) { this.name = n; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal p) { this.price = p; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer q) { this.quantity = q; }
    }

    @Data
    public static class TicketTypeResponse {
        private Long id;
        private String name;
        private BigDecimal price;
        private Integer availableQuantity;
        private Integer soldQuantity;

        public Long getId() { return this.id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return this.name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getPrice() { return this.price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public Integer getAvailableQuantity() { return this.availableQuantity; }
        public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }
        public Integer getSoldQuantity() { return this.soldQuantity; }
        public void setSoldQuantity(Integer soldQuantity) { this.soldQuantity = soldQuantity; }
    }
}
