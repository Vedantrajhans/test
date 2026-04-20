package com.vedant.concert_platform.controller;

import com.vedant.concert_platform.dto.ConcertDto;
import com.vedant.concert_platform.dto.PageResponse;
import com.vedant.concert_platform.entity.enums.Status;
import com.vedant.concert_platform.service.ConcertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ConcertController {

    private final ConcertService concertService;

    // Public search
    @GetMapping("/concerts")
    public ResponseEntity<PageResponse<ConcertDto.ConcertResponse>> searchConcerts(
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            Pageable pageable) {
        return ResponseEntity.ok(concertService.searchConcerts(status, genre, dateFrom, dateTo, pageable));
    }

    @GetMapping("/concerts/{id}")
    public ResponseEntity<ConcertDto.ConcertResponse> getConcert(@PathVariable Long id) {
        return ResponseEntity.ok(concertService.getConcert(id));
    }

    // Organizer Only
    @GetMapping("/organizer/concerts")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<PageResponse<ConcertDto.ConcertResponse>> getMyConcerts(Pageable pageable) {
        return ResponseEntity.ok(concertService.getMyConcerts(pageable));
    }

    @PostMapping("/organizer/concerts")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ConcertDto.ConcertResponse> createConcert(@Valid @RequestBody ConcertDto.ConcertRequest request) {
        return ResponseEntity.ok(concertService.createConcert(request));
    }

    @PutMapping("/organizer/concerts/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ConcertDto.ConcertResponse> updateConcert(@PathVariable Long id, @Valid @RequestBody ConcertDto.ConcertRequest request) {
        return ResponseEntity.ok(concertService.updateConcert(id, request));
    }

    @PatchMapping("/organizer/concerts/{id}/publish")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<Void> publishConcert(@PathVariable Long id) {
        concertService.publishConcert(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/organizer/concerts/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<Void> deleteConcert(@PathVariable Long id) {
        concertService.deleteConcert(id);
        return ResponseEntity.ok().build();
    }

    // Fix #12: Export attendee CSV for capacity < 500
    @GetMapping("/organizer/concerts/{id}/attendees/export")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<byte[]> exportAttendees(@PathVariable Long id) {
        byte[] csv = concertService.exportAttendeeCsv(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=attendees.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
