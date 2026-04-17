package com.vedant.concert_platform.repository;

import com.vedant.concert_platform.entity.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketTypeRepository extends JpaRepository<TicketType, Long> {
    List<TicketType> findByConcertId(Long concertId);
}
