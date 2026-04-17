package com.vedant.concert_platform.controller;

import com.vedant.concert_platform.entity.Promoter;
import com.vedant.concert_platform.service.PromoterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promoters")
@RequiredArgsConstructor
public class PromoterController {

    private final PromoterService promoterService;

    @GetMapping
    public ResponseEntity<List<Promoter>> getAll() {
        return ResponseEntity.ok(promoterService.getAll());
    }

    @PostMapping
    public ResponseEntity<Promoter> create(@RequestBody Promoter promoter) {
        return ResponseEntity.ok(promoterService.create(promoter));
    }
}
