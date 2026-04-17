package com.vedant.concert_platform.repository;

import com.vedant.concert_platform.entity.Promoter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromoterRepository extends JpaRepository<Promoter, Long> {
}
