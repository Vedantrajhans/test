package com.vedant.concert_platform.repository;

import com.vedant.concert_platform.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    Page<Feedback> findByConcertId(Long concertId, Pageable pageable);

    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.concert.id = :concertId")
    Double getAverageRatingForConcert(@Param("concertId") Long concertId);

    @Query("SELECT COUNT(f) > 0 FROM Feedback f WHERE f.user.id = :userId AND f.concert.id = :concertId")
    boolean existsByUserIdAndConcertId(@Param("userId") Long userId, @Param("concertId") Long concertId);

    @Query("SELECT f FROM Feedback f WHERE f.user.id = :userId AND f.concert.id = :concertId")
    Optional<Feedback> findByUserIdAndConcertId(@Param("userId") Long userId, @Param("concertId") Long concertId);
}
