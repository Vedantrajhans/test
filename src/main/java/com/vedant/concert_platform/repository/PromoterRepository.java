package com.vedant.concert_platform.repository;

import com.vedant.concert_platform.entity.Promoter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PromoterRepository extends JpaRepository<Promoter, Long> {
    Optional<Promoter> findByUserId(Long userId);
    List<Promoter> findByProducerId(Long producerId);
}
