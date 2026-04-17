package com.vedant.concert_platform.controller;

import com.vedant.concert_platform.dto.HierarchyDto;
import com.vedant.concert_platform.service.ProducerManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
