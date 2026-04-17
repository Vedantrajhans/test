package com.vedant.concert_platform.repository;

import com.vedant.concert_platform.entity.TicketBooking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TicketBookingRepository extends JpaRepository<TicketBooking, Long> {
    Optional<TicketBooking> findByUuid(UUID uuid);
    Optional<TicketBooking> findByBookingReference(String bookingReference);
    Page<TicketBooking> findByUserId(Long userId, Pageable pageable);
    Page<TicketBooking> findByConcertId(Long concertId, Pageable pageable);
}
