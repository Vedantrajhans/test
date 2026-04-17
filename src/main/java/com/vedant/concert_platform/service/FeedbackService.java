package com.vedant.concert_platform.service;

import com.vedant.concert_platform.dto.FeedbackDto;
import com.vedant.concert_platform.entity.Concert;
import com.vedant.concert_platform.entity.Feedback;
import com.vedant.concert_platform.entity.User;
import com.vedant.concert_platform.exception.ConflictException;
import com.vedant.concert_platform.exception.ResourceNotFoundException;
import com.vedant.concert_platform.repository.ConcertRepository;
import com.vedant.concert_platform.repository.FeedbackRepository;
import com.vedant.concert_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final ConcertRepository concertRepository;

    @Transactional
    public FeedbackDto.Response addFeedback(FeedbackDto.CreateRequest request) {
        User user = getCurrentUser();
        Concert concert = concertRepository.findById(request.getConcertId())
                .orElseThrow(() -> new ResourceNotFoundException("Concert not found"));

        Feedback feedback = new Feedback();
        feedback.setUser(user);
        feedback.setConcert(concert);
        feedback.setRating(request.getRating());
        feedback.setComment(request.getComment());
        feedback.setSoundQuality(request.getSoundQuality());
        feedback.setVenueExperience(request.getVenueExperience());
        feedback.setArtistPerformance(request.getArtistPerformance());

        try {
            return map(feedbackRepository.save(feedback));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Feedback already submitted for this concert");
        }
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private FeedbackDto.Response map(Feedback feedback) {
        FeedbackDto.Response response = new FeedbackDto.Response();
        response.setId(feedback.getId());
        response.setConcertId(feedback.getConcert().getId());
        response.setRating(feedback.getRating());
        response.setComment(feedback.getComment());
        response.setSoundQuality(feedback.getSoundQuality());
        response.setVenueExperience(feedback.getVenueExperience());
        response.setArtistPerformance(feedback.getArtistPerformance());
        return response;
    }
}
