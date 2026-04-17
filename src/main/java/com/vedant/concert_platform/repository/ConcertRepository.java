package com.vedant.concert_platform.repository;

import com.vedant.concert_platform.entity.Concert;
import com.vedant.concert_platform.entity.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

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
}
