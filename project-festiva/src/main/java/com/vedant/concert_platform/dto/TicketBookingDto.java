package com.vedant.concert_platform.dto;

import com.vedant.concert_platform.entity.enums.BookingStatus;
import com.vedant.concert_platform.entity.enums.PaymentStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TicketBookingDto {

    @Data
    public static class BookingRequest {
        @NotNull private Long concertId;
        @NotNull private Long ticketTypeId;
        @NotNull @Min(1) private Integer quantity;
        public Long getConcertId() { return concertId; }
        public void setConcertId(Long c) { this.concertId = c; }
        public Long getTicketTypeId() { return ticketTypeId; }
        public void setTicketTypeId(Long t) { this.ticketTypeId = t; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer q) { this.quantity = q; }
    }

    @Data
    public static class PaymentConfirmRequest {
        @NotNull private UUID bookingUuid;
        private String paymentReference;
        public UUID getBookingUuid() { return bookingUuid; }
        public void setBookingUuid(UUID u) { this.bookingUuid = u; }
        public String getPaymentReference() { return paymentReference; }
        public void setPaymentReference(String p) { this.paymentReference = p; }
    }

    @Data
    public static class BookingResponse {
        private UUID uuid;
        private Long concertId;
        private String concertTitle;
        private LocalDateTime concertDateTime;
        private String venueCity;
        private Long ticketTypeId;
        private String ticketTypeName;
        private Integer quantity;
        private BigDecimal totalAmount;
        private PaymentStatus paymentStatus;
        private BookingStatus bookingStatus;
        private String bookingReference;
        private String qrCode;
        public UUID getUuid() { return uuid; }
        public void setUuid(UUID u) { this.uuid = u; }
        public Long getConcertId() { return concertId; }
        public void setConcertId(Long c) { this.concertId = c; }
        public String getConcertTitle() { return concertTitle; }
        public void setConcertTitle(String c) { this.concertTitle = c; }
        public LocalDateTime getConcertDateTime() { return concertDateTime; }
        public void setConcertDateTime(LocalDateTime d) { this.concertDateTime = d; }
        public String getVenueCity() { return venueCity; }
        public void setVenueCity(String v) { this.venueCity = v; }
        public Long getTicketTypeId() { return ticketTypeId; }
        public void setTicketTypeId(Long t) { this.ticketTypeId = t; }
        public String getTicketTypeName() { return ticketTypeName; }
        public void setTicketTypeName(String t) { this.ticketTypeName = t; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer q) { this.quantity = q; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal t) { this.totalAmount = t; }
        public PaymentStatus getPaymentStatus() { return paymentStatus; }
        public void setPaymentStatus(PaymentStatus p) { this.paymentStatus = p; }
        public BookingStatus getBookingStatus() { return bookingStatus; }
        public void setBookingStatus(BookingStatus b) { this.bookingStatus = b; }
        public String getBookingReference() { return bookingReference; }
        public void setBookingReference(String r) { this.bookingReference = r; }
        public String getQrCode() { return qrCode; }
        public void setQrCode(String q) { this.qrCode = q; }
    }
}
