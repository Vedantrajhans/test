package com.vedant.concert_platform.controller;

import com.vedant.concert_platform.dto.HierarchyDto;
import com.vedant.concert_platform.dto.PageResponse;
import com.vedant.concert_platform.entity.enums.Role;
import com.vedant.concert_platform.entity.enums.Status;
import com.vedant.concert_platform.service.ProducerManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/producer")
@RequiredArgsConstructor
public class ProducerController {

    private final ProducerManagementService producerManagementService;

    @PostMapping("/promoters")
    @PreAuthorize("hasRole('PRODUCER')")
    public ResponseEntity<HierarchyDto.PromoterResponse> createPromoter(@Valid @RequestBody HierarchyDto.CreatePromoterRequest request) {
        return ResponseEntity.ok(producerManagementService.createPromoter(request));
    }

    @GetMapping("/promoters")
    @PreAuthorize("hasRole('PRODUCER')")
    public ResponseEntity<List<HierarchyDto.PromoterResponse>> listPromoters() {
        return ResponseEntity.ok(producerManagementService.listPromoters());
    }

    @PutMapping("/promoters/{promoterId}")
    @PreAuthorize("hasRole('PRODUCER')")
    public ResponseEntity<HierarchyDto.PromoterResponse> updatePromoter(
            @PathVariable Long promoterId,
            @Valid @RequestBody HierarchyDto.UpdatePromoterRequest request) {
        return ResponseEntity.ok(producerManagementService.updatePromoter(promoterId, request));
    }

    /** Soft deactivate */
    @DeleteMapping("/promoters/{promoterId}")
    @PreAuthorize("hasRole('PRODUCER')")
    public ResponseEntity<Void> deactivatePromoter(@PathVariable Long promoterId) {
        producerManagementService.deactivatePromoter(promoterId);
        return ResponseEntity.ok().build();
    }

    /** Permanent hard delete */
    @DeleteMapping("/promoters/{promoterId}/hard")
    @PreAuthorize("hasRole('PRODUCER')")
    public ResponseEntity<Map<String, String>> hardDeletePromoter(@PathVariable Long promoterId) {
        producerManagementService.hardDeletePromoter(promoterId);
        return ResponseEntity.ok(Map.of("message", "Promoter permanently deleted"));
    }

    /** List all platform users */
    @GetMapping("/users")
    @PreAuthorize("hasRole('PRODUCER')")
    public ResponseEntity<PageResponse<HierarchyDto.UserSummary>> listAllUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Status status,
            Pageable pageable) {
        return ResponseEntity.ok(new PageResponse<>(producerManagementService.listAllUsers(role, status, pageable)));
    }

    /** Hard delete any user by ID */
    @DeleteMapping("/users/{userId}/hard")
    @PreAuthorize("hasRole('PRODUCER')")
    public ResponseEntity<Map<String, String>> hardDeleteUser(@PathVariable Long userId) {
        producerManagementService.hardDeleteUser(userId);
        return ResponseEntity.ok(Map.of("message", "User permanently deleted"));
    }
}
