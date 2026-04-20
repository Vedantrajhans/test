package com.vedant.concert_platform.controller;

import com.vedant.concert_platform.dto.FeedbackDto;
import com.vedant.concert_platform.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    @PreAuthorize("hasRole('ATTENDEE')")
    public ResponseEntity<FeedbackDto.Response> addFeedback(@Valid @RequestBody FeedbackDto.CreateRequest request) {
        return ResponseEntity.ok(feedbackService.addFeedback(request));
    }

    @PutMapping("/{feedbackId}")
    @PreAuthorize("hasRole('ATTENDEE')")
    public ResponseEntity<FeedbackDto.Response> updateFeedback(
            @PathVariable Long feedbackId,
            @Valid @RequestBody FeedbackDto.CreateRequest request) {
        return ResponseEntity.ok(feedbackService.updateFeedback(feedbackId, request));
    }

    @DeleteMapping("/{feedbackId}")
    @PreAuthorize("hasRole('ATTENDEE')")
    public ResponseEntity<Map<String, String>> deleteFeedback(@PathVariable Long feedbackId) {
        feedbackService.deleteFeedback(feedbackId);
        return ResponseEntity.ok(Map.of("message", "Review deleted"));
    }

    @GetMapping("/concert/{concertId}")
    public ResponseEntity<List<FeedbackDto.Response>> getFeedbackForConcert(@PathVariable Long concertId) {
        return ResponseEntity.ok(feedbackService.getFeedbackForConcert(concertId));
    }

    @GetMapping("/concert/{concertId}/my")
    @PreAuthorize("hasRole('ATTENDEE')")
    public ResponseEntity<FeedbackDto.Response> getMyFeedback(@PathVariable Long concertId) {
        return ResponseEntity.ok(feedbackService.getMyFeedbackForConcert(concertId));
    }
}
