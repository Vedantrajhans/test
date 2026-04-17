package com.vedant.concert_platform.repository;

import com.vedant.concert_platform.entity.Producer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProducerRepository extends JpaRepository<Producer, Long> {
}
