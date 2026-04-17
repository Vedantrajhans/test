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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final TicketBookingRepository bookingRepository;
    private final ConcertRepository concertRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final UserRepository userRepository;

    // Use transactional isolation and optimistic locking (via @Version on Concert and TicketType)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TicketBookingDto.BookingResponse reserveTickets(TicketBookingDto.BookingRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(username).orElseThrow();

        try {
            Concert concert = concertRepository.findByIdForUpdate(request.getConcertId())
                    .orElseThrow(() -> new ResourceNotFoundException("Concert not found"));
            TicketType ticketType = ticketTypeRepository.findByIdForUpdate(request.getTicketTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket Type not found"));

            if (!ticketType.getConcert().getId().equals(concert.getId())) {
                throw new BadRequestException("Ticket type does not belong to this concert");
            }

            int availableTickets = ticketType.getQuantityAvailable() - ticketType.getQuantitySold();
            if (request.getQuantity() > availableTickets) {
                throw new ConflictException("Not enough tickets available for this tier");
            }

            if (concert.getTicketsSold() + request.getQuantity() > concert.getTotalCapacity()) {
                throw new ConflictException("Concert has reached total capacity");
            }

            ticketType.setQuantitySold(ticketType.getQuantitySold() + request.getQuantity());
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
            TicketBooking savedBooking = bookingRepository.save(booking);

            return mapToResponse(savedBooking);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ConflictException("Concurrent booking detected. Please retry.");
        }
    }

    @Transactional
    public TicketBookingDto.BookingResponse confirmPayment(TicketBookingDto.PaymentConfirmRequest request) {
        TicketBooking booking = bookingRepository.findByUuid(request.getBookingUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Booking already paid");
        }

        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setBookingReference("REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        booking.setQrCode("QR-DATA-PLACEHOLDER-" + booking.getUuid());

        return mapToResponse(bookingRepository.save(booking));
    }

    public PageResponse<TicketBookingDto.BookingResponse> getMyBookings(Pageable pageable) {
        User user = getCurrentUser();
        Page<TicketBooking> bookings = bookingRepository.findByUserId(user.getId(), pageable);
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
