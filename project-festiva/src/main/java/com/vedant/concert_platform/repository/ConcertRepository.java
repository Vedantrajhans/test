package com.vedant.concert_platform.repository;

import com.vedant.concert_platform.entity.Concert;
import com.vedant.concert_platform.entity.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConcertRepository extends JpaRepository<Concert, Long> {

    Page<Concert> findByOrganizerId(Long organizerId, Pageable pageable);

    @Query("SELECT c FROM Concert c WHERE " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:genre IS NULL OR c.genre = :genre) AND " +
            "(cast(:dateFrom as timestamp) IS NULL OR c.dateTime >= :dateFrom) AND " +
            "(cast(:dateTo as timestamp) IS NULL OR c.dateTime <= :dateTo)")
    Page<Concert> searchConcerts(@Param("status") Status status,
                                 @Param("genre") String genre,
                                 @Param("dateFrom") LocalDateTime dateFrom,
                                 @Param("dateTo") LocalDateTime dateTo,
                                 Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Concert c WHERE c.id = :id")
    Optional<Concert> findByIdForUpdate(@Param("id") Long id);

    // Fetch single concert with venue + organizer eagerly (used in getConcert)
    @Query("SELECT c FROM Concert c LEFT JOIN FETCH c.venue LEFT JOIN FETCH c.organizer WHERE c.id = :id")
    Optional<Concert> findByIdWithDetails(@Param("id") Long id);

    // Fix #25: batch-fetch venue for a set of concert IDs to avoid N+1 on list pages
    @Query("SELECT c FROM Concert c LEFT JOIN FETCH c.venue LEFT JOIN FETCH c.organizer WHERE c.id IN :ids")
    List<Concert> findByIdInWithDetails(@Param("ids") List<Long> ids);
}