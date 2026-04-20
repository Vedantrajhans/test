package com.vedant.concert_platform.controller;

import com.vedant.concert_platform.dto.PageResponse;
import com.vedant.concert_platform.dto.TicketBookingDto;
import com.vedant.concert_platform.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/reserve")
    @PreAuthorize("hasRole('ATTENDEE')")
    public ResponseEntity<TicketBookingDto.BookingResponse> reserveTickets(@Valid @RequestBody TicketBookingDto.BookingRequest request) {
        return ResponseEntity.ok(bookingService.reserveTickets(request));
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasRole('ATTENDEE')")
    public ResponseEntity<TicketBookingDto.BookingResponse> confirmPayment(@Valid @RequestBody TicketBookingDto.PaymentConfirmRequest request) {
        return ResponseEntity.ok(bookingService.confirmPayment(request));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('ATTENDEE')")
    public ResponseEntity<PageResponse<TicketBookingDto.BookingResponse>> getMyBookings(Pageable pageable) {
        return ResponseEntity.ok(bookingService.getMyBookings(pageable));
    }

    // Fix #12: booking cancellation endpoint
    @PostMapping("/{bookingUuid}/cancel")
    @PreAuthorize("hasRole('ATTENDEE')")
    public ResponseEntity<TicketBookingDto.BookingResponse> cancelBooking(@PathVariable UUID bookingUuid) {
        return ResponseEntity.ok(bookingService.cancelBooking(bookingUuid));
    }
}
