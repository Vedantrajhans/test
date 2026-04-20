package com.vedant.concert_platform.repository;

import com.vedant.concert_platform.entity.TicketBooking;
import com.vedant.concert_platform.entity.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketBookingRepository extends JpaRepository<TicketBooking, Long> {

    Optional<TicketBooking> findByUuid(UUID uuid);
    Optional<TicketBooking> findByBookingReference(String bookingReference);

    // Fix: eager-load concert, venue, ticketType to avoid LazyInitializationException
    @Query("SELECT b FROM TicketBooking b JOIN FETCH b.concert c LEFT JOIN FETCH c.venue JOIN FETCH b.ticketType WHERE b.user.id = :userId")
    Page<TicketBooking> findByUserIdWithDetails(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT b FROM TicketBooking b WHERE b.concert.id = :concertId AND b.bookingStatus <> 'CANCELLED'")
    List<TicketBooking> findActiveByConcertId(@Param("concertId") Long concertId);

    @Query("SELECT b FROM TicketBooking b JOIN FETCH b.user WHERE b.concert.id = :concertId AND b.bookingStatus = 'CONFIRMED'")
    List<TicketBooking> findConfirmedByConcertId(@Param("concertId") Long concertId);

    @Query("SELECT b FROM TicketBooking b JOIN FETCH b.user JOIN FETCH b.ticketType WHERE b.concert.id = :concertId AND b.bookingStatus = 'CONFIRMED'")
    List<TicketBooking> findConfirmedByConcertIdWithDetails(@Param("concertId") Long concertId);

    @Query("SELECT b FROM TicketBooking b WHERE b.uuid = :uuid AND b.user.id = :userId")
    Optional<TicketBooking> findByUuidAndUserId(@Param("uuid") UUID uuid, @Param("userId") Long userId);

    @Query("SELECT COUNT(b) > 0 FROM TicketBooking b WHERE b.user.id = :userId AND b.concert.id = :concertId AND b.bookingStatus <> :status")
    boolean existsByUserIdAndConcertIdAndBookingStatusNot(
            @Param("userId") Long userId, @Param("concertId") Long concertId, @Param("status") BookingStatus status);

    @Query("SELECT COUNT(b) FROM TicketBooking b WHERE b.user.id = :userId AND b.concert.id = :concertId AND b.bookingStatus = :status")
    long countByUserIdAndConcertIdAndBookingStatus(
            @Param("userId") Long userId, @Param("concertId") Long concertId, @Param("status") BookingStatus status);
}
