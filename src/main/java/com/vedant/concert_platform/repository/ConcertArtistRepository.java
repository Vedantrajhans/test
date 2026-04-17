package com.vedant.concert_platform.repository;

import com.vedant.concert_platform.entity.ConcertArtist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConcertArtistRepository extends JpaRepository<ConcertArtist, Long> {
    List<ConcertArtist> findByConcertIdOrderByPerformanceOrderAsc(Long concertId);
}
