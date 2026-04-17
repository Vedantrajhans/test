package com.vedant.concert_platform.controller;

import com.vedant.concert_platform.entity.Producer;
import com.vedant.concert_platform.service.ProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/producers")
@RequiredArgsConstructor
public class ProducerController {

    private final ProducerService producerService;

    @GetMapping
    public ResponseEntity<List<Producer>> getAll() {
        return ResponseEntity.ok(producerService.getAll());
    }

    @PostMapping
    public ResponseEntity<Producer> create(@RequestBody Producer producer) {
        return ResponseEntity.ok(producerService.create(producer));
    }
}
