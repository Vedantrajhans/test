package com.vedant.concert_platform.dto;

import com.vedant.concert_platform.entity.enums.BookingStatus;
import com.vedant.concert_platform.entity.enums.PaymentStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

public class TicketBookingDto {

    @Data
    public static class BookingRequest {
        @NotNull
        private Long concertId;

        @NotNull
        private Long ticketTypeId;

        @NotNull
        @Min(1)
        private Integer quantity;
        public Long getConcertId() { return this.concertId; }
        public void setConcertId(Long concertId) { this.concertId = concertId; }
        public Long getTicketTypeId() { return this.ticketTypeId; }
        public void setTicketTypeId(Long ticketTypeId) { this.ticketTypeId = ticketTypeId; }
        public Integer getQuantity() { return this.quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
    
    @Data
    public static class PaymentConfirmRequest {
        @NotNull
        private UUID bookingUuid;
        // In real world, we would receive payment gateway signature here
        private String paymentReference;
        public UUID getBookingUuid() { return this.bookingUuid; }
        public void setBookingUuid(UUID bookingUuid) { this.bookingUuid = bookingUuid; }
        public String getPaymentReference() { return this.paymentReference; }
        public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
    }

    @Data
    public static class BookingResponse {
        private UUID uuid;
        private Long concertId;
        private String concertTitle;
        private Long ticketTypeId;
        private String ticketTypeName;
        private Integer quantity;
        private BigDecimal totalAmount;
        private PaymentStatus paymentStatus;
        private BookingStatus bookingStatus;
        private String bookingReference;
        private String qrCode;
        public UUID getUuid() { return this.uuid; }
        public void setUuid(UUID uuid) { this.uuid = uuid; }
        public Long getConcertId() { return this.concertId; }
        public void setConcertId(Long concertId) { this.concertId = concertId; }
        public String getConcertTitle() { return this.concertTitle; }
        public void setConcertTitle(String concertTitle) { this.concertTitle = concertTitle; }
        public Long getTicketTypeId() { return this.ticketTypeId; }
        public void setTicketTypeId(Long ticketTypeId) { this.ticketTypeId = ticketTypeId; }
        public String getTicketTypeName() { return this.ticketTypeName; }
        public void setTicketTypeName(String ticketTypeName) { this.ticketTypeName = ticketTypeName; }
        public Integer getQuantity() { return this.quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public BigDecimal getTotalAmount() { return this.totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public PaymentStatus getPaymentStatus() { return this.paymentStatus; }
        public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
        public BookingStatus getBookingStatus() { return this.bookingStatus; }
        public void setBookingStatus(BookingStatus bookingStatus) { this.bookingStatus = bookingStatus; }
        public String getBookingReference() { return this.bookingReference; }
        public void setBookingReference(String bookingReference) { this.bookingReference = bookingReference; }
        public String getQrCode() { return this.qrCode; }
        public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    }
}
