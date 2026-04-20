package com.vedant.concert_platform.service;

import com.vedant.concert_platform.dto.PageResponse;
import com.vedant.concert_platform.dto.TicketBookingDto;
import com.vedant.concert_platform.entity.Concert;
import com.vedant.concert_platform.entity.TicketBooking;
import com.vedant.concert_platform.entity.TicketType;
import com.vedant.concert_platform.entity.User;
import com.vedant.concert_platform.entity.enums.BookingStatus;
import com.vedant.concert_platform.entity.enums.PaymentStatus;
import com.vedant.concert_platform.exception.BadRequestException;
import com.vedant.concert_platform.exception.ConflictException;
import com.vedant.concert_platform.exception.ResourceNotFoundException;
import com.vedant.concert_platform.repository.ConcertRepository;
import com.vedant.concert_platform.repository.TicketBookingRepository;
import com.vedant.concert_platform.repository.TicketTypeRepository;
import com.vedant.concert_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final TicketBookingRepository bookingRepository;
    private final ConcertRepository concertRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TicketBookingDto.BookingResponse reserveTickets(TicketBookingDto.BookingRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(username).orElseThrow();

        try {
            Concert concert = concertRepository.findByIdForUpdate(request.getConcertId())
                    .orElseThrow(() -> new ResourceNotFoundException("Concert not found"));
            TicketType ticketType = ticketTypeRepository.findByIdForUpdate(request.getTicketTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket Type not found"));

            LocalDateTime now = LocalDateTime.now();
            if (concert.getTicketSaleStart() != null && now.isBefore(concert.getTicketSaleStart())) {
                throw new BadRequestException("Ticket sales have not started yet. Sales open at: " + concert.getTicketSaleStart());
            }
            if (concert.getTicketSaleEnd() != null && now.isAfter(concert.getTicketSaleEnd())) {
                throw new BadRequestException("Ticket sales have closed for this concert.");
            }

            boolean hasExistingBooking = bookingRepository.existsByUserIdAndConcertIdAndBookingStatusNot(
                    user.getId(), concert.getId(), BookingStatus.CANCELLED);
            if (hasExistingBooking) {
                throw new ConflictException("You already have an active booking for this concert.");
            }

            if (!ticketType.getConcert().getId().equals(concert.getId())) {
                throw new BadRequestException("Ticket type does not belong to this concert");
            }

            // Check for sold-out ticket type
            int availableTickets = ticketType.getQuantityAvailable() - ticketType.getQuantitySold();
            if (availableTickets <= 0) {
                throw new ConflictException("Sorry, " + ticketType.getName() + " tickets are SOLD OUT.");
            }
            if (request.getQuantity() > availableTickets) {
                throw new ConflictException("Only " + availableTickets + " " + ticketType.getName() + " ticket(s) remaining.");
            }

            if (concert.getTicketsSold() + request.getQuantity() > concert.getTotalCapacity()) {
                throw new ConflictException("Concert has reached total capacity");
            }

            ticketType.setQuantitySold(ticketType.getQuantitySold() + request.getQuantity());
            ticketType.setQuantityAvailable(ticketType.getQuantityAvailable() - request.getQuantity());
            concert.setTicketsSold(concert.getTicketsSold() + request.getQuantity());
            ticketTypeRepository.save(ticketType);
            concertRepository.save(concert);

            TicketBooking booking = new TicketBooking();
            booking.setUuid(UUID.randomUUID());
            booking.setUser(user);
            booking.setConcert(concert);
            booking.setTicketType(ticketType);
            booking.setQuantity(request.getQuantity());

            BigDecimal totalAmount = ticketType.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
            booking.setTotalAmount(totalAmount);
            booking.setPaymentStatus(PaymentStatus.PENDING);
            booking.setBookingStatus(BookingStatus.PENDING);
            return mapToResponse(bookingRepository.save(booking));
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ConflictException("Concurrent booking detected. Please retry.");
        }
    }

    @Transactional
    public TicketBookingDto.BookingResponse confirmPayment(TicketBookingDto.PaymentConfirmRequest request) {
        User user = getCurrentUser();
        TicketBooking booking = bookingRepository.findByUuidAndUserId(request.getBookingUuid(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Booking already paid");
        }

        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setBookingReference("REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        booking.setQrCode("FESTIVA:" + booking.getUuid().toString().toUpperCase());

        TicketBooking saved = bookingRepository.save(booking);

        // Send confirmation email with QR code
        emailService.sendBookingConfirmation(saved);

        return mapToResponse(saved);
    }

    @Transactional
    public TicketBookingDto.BookingResponse cancelBooking(UUID bookingUuid) {
        User user = getCurrentUser();
        TicketBooking booking = bookingRepository.findByUuidAndUserId(bookingUuid, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking is already cancelled");
        }
        if (booking.getBookingStatus() == BookingStatus.CONFIRMED && booking.getPaymentStatus() == PaymentStatus.PAID) {
            booking.setPaymentStatus(PaymentStatus.REFUNDED);
        }

        TicketType ticketType = booking.getTicketType();
        ticketType.setQuantitySold(ticketType.getQuantitySold() - booking.getQuantity());
        ticketType.setQuantityAvailable(ticketType.getQuantityAvailable() + booking.getQuantity());
        ticketTypeRepository.save(ticketType);

        Concert concert = booking.getConcert();
        concert.setTicketsSold(Math.max(0, concert.getTicketsSold() - booking.getQuantity()));
        concertRepository.save(concert);

        booking.setBookingStatus(BookingStatus.CANCELLED);
        return mapToResponse(bookingRepository.save(booking));
    }

    public PageResponse<TicketBookingDto.BookingResponse> getMyBookings(Pageable pageable) {
        User user = getCurrentUser();
        Page<TicketBooking> bookings = bookingRepository.findByUserIdWithDetails(user.getId(), pageable);
        return new PageResponse<>(bookings.map(this::mapToResponse));
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(username).orElseThrow();
    }

    private TicketBookingDto.BookingResponse mapToResponse(TicketBooking booking) {
        TicketBookingDto.BookingResponse res = new TicketBookingDto.BookingResponse();
        res.setUuid(booking.getUuid());
        res.setConcertId(booking.getConcert().getId());
        res.setConcertTitle(booking.getConcert().getTitle());
        res.setConcertDateTime(booking.getConcert().getDateTime());
        res.setVenueCity(booking.getConcert().getVenue() != null ? booking.getConcert().getVenue().getCity() : null);
        res.setTicketTypeId(booking.getTicketType().getId());
        res.setTicketTypeName(booking.getTicketType().getName());
        res.setQuantity(booking.getQuantity());
        res.setTotalAmount(booking.getTotalAmount());
        res.setPaymentStatus(booking.getPaymentStatus());
        res.setBookingStatus(booking.getBookingStatus());
        res.setBookingReference(booking.getBookingReference());
        res.setQrCode(booking.getQrCode());
        return res;
    }
}
