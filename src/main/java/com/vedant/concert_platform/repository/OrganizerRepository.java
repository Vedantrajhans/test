package com.vedant.concert_platform.repository;

import com.vedant.concert_platform.entity.Organizer;
import com.vedant.concert_platform.entity.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface OrganizerRepository extends JpaRepository<Organizer, Long> {
    Optional<Organizer> findByUserId(Long userId);
    Optional<Organizer> findByUserEmail(String email);
    List<Organizer> findByPromoterId(Long promoterId);

    @Query("SELECT o FROM Organizer o WHERE " +
            "(:status IS NULL OR o.status = :status) AND " +
            "(:organizerType IS NULL OR o.organizerType = :organizerType) AND " +
            "(:city IS NULL OR o.city = :city)")
    Page<Organizer> findWithFilters(@Param("status") Status status,
                                    @Param("organizerType") String organizerType,
                                    @Param("city") String city,
                                    Pageable pageable);
}
