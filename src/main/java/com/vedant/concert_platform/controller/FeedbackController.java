package com.vedant.concert_platform.controller;

import com.vedant.concert_platform.dto.FeedbackDto;
import com.vedant.concert_platform.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
}
