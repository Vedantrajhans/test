package com.vedant.concert_platform.repository;

import com.vedant.concert_platform.entity.Producer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProducerRepository extends JpaRepository<Producer, Long> {
    Optional<Producer> findByUserId(Long userId);
}
