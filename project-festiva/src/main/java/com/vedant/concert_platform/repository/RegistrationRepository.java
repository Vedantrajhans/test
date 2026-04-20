package com.vedant.concert_platform.repository;

import com.vedant.concert_platform.entity.Registration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    Page<Registration> findByConcertId(Long concertId, Pageable pageable);
    boolean existsByUserIdAndConcertId(Long userId, Long concertId);
}
