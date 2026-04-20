package com.vedant.concert_platform.repository;

import com.vedant.concert_platform.entity.Artist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistRepository extends JpaRepository<Artist, Long> {
}
