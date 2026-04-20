package com.vedant.concert_platform.repository;

import com.vedant.concert_platform.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueRepository extends JpaRepository<Venue, Long> {
}
