package com.vedant.concert_platform.repository;

import com.vedant.concert_platform.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    Page<Feedback> findByConcertId(Long concertId, Pageable pageable);

    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.concert.id = :concertId")
    Double getAverageRatingForConcert(@Param("concertId") Long concertId);
}
