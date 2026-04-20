package com.vedant.concert_platform.entity;

import com.vedant.concert_platform.entity.enums.BookingStatus;
import com.vedant.concert_platform.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "ticket_bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketBooking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_type_id", nullable = false)
    private TicketType ticketType;

    private Integer quantity;

    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BookingStatus bookingStatus = BookingStatus.CONFIRMED;

    @Column(unique = true)
    private String bookingReference;

    @Column(columnDefinition = "TEXT")
    private String qrCode;

    @Version
    private Integer version;

    @PrePersist
    public void generateUUID() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }
    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public UUID getUuid() { return this.uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }
    public User getUser() { return this.user; }
    public void setUser(User user) { this.user = user; }
    public Concert getConcert() { return this.concert; }
    public void setConcert(Concert concert) { this.concert = concert; }
    public TicketType getTicketType() { return this.ticketType; }
    public void setTicketType(TicketType ticketType) { this.ticketType = ticketType; }
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
    public Integer getVersion() { return this.version; }
    public void setVersion(Integer version) { this.version = version; }
}