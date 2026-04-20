package com.vedant.concert_platform.service;

import com.vedant.concert_platform.dto.FeedbackDto;
import com.vedant.concert_platform.entity.Concert;
import com.vedant.concert_platform.entity.Feedback;
import com.vedant.concert_platform.entity.User;
import com.vedant.concert_platform.entity.enums.BookingStatus;
import com.vedant.concert_platform.exception.BadRequestException;
import com.vedant.concert_platform.exception.ConflictException;
import com.vedant.concert_platform.exception.ResourceNotFoundException;
import com.vedant.concert_platform.repository.ConcertRepository;
import com.vedant.concert_platform.repository.FeedbackRepository;
import com.vedant.concert_platform.repository.TicketBookingRepository;
import com.vedant.concert_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final ConcertRepository concertRepository;
    private final TicketBookingRepository ticketBookingRepository;

    @Transactional
    public FeedbackDto.Response addFeedback(FeedbackDto.CreateRequest request) {
        User user = getCurrentUser();
        Concert concert = concertRepository.findById(request.getConcertId())
                .orElseThrow(() -> new ResourceNotFoundException("Concert not found"));

        long confirmedBookings = ticketBookingRepository.countByUserIdAndConcertIdAndBookingStatus(
                user.getId(), concert.getId(), BookingStatus.CONFIRMED);
        if (confirmedBookings == 0) {
            throw new BadRequestException("You can only review concerts you have attended with a confirmed booking.");
        }

        if (feedbackRepository.existsByUserIdAndConcertId(user.getId(), concert.getId())) {
            throw new ConflictException("You have already submitted feedback for this concert.");
        }

        Feedback feedback = new Feedback();
        feedback.setUser(user);
        feedback.setConcert(concert);
        feedback.setRating(request.getRating());
        feedback.setComment(request.getComment());
        feedback.setSoundQuality(request.getSoundQuality());
        feedback.setVenueExperience(request.getVenueExperience());
        feedback.setArtistPerformance(request.getArtistPerformance());
        return map(feedbackRepository.save(feedback));
    }

    @Transactional
    public FeedbackDto.Response updateFeedback(Long feedbackId, FeedbackDto.CreateRequest request) {
        User user = getCurrentUser();
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        if (!feedback.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You can only edit your own reviews");
        }
        feedback.setRating(request.getRating());
        feedback.setComment(request.getComment());
        feedback.setSoundQuality(request.getSoundQuality());
        feedback.setVenueExperience(request.getVenueExperience());
        feedback.setArtistPerformance(request.getArtistPerformance());
        return map(feedbackRepository.save(feedback));
    }

    @Transactional
    public void deleteFeedback(Long feedbackId) {
        User user = getCurrentUser();
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        if (!feedback.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You can only delete your own reviews");
        }
        feedbackRepository.delete(feedback);
    }

    @Transactional(readOnly = true)
    public List<FeedbackDto.Response> getFeedbackForConcert(Long concertId) {
        concertRepository.findById(concertId)
                .orElseThrow(() -> new ResourceNotFoundException("Concert not found"));
        return feedbackRepository.findByConcertId(concertId, PageRequest.of(0, 100))
                .stream().map(this::map).toList();
    }

    /** Returns user's own review for a concert, or null */
    @Transactional(readOnly = true)
    public FeedbackDto.Response getMyFeedbackForConcert(Long concertId) {
        User user = getCurrentUser();
        return feedbackRepository.findByUserIdAndConcertId(user.getId(), concertId)
                .map(this::map).orElse(null);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private FeedbackDto.Response map(Feedback feedback) {
        FeedbackDto.Response response = new FeedbackDto.Response();
        response.setId(feedback.getId());
        response.setConcertId(feedback.getConcert().getId());
        response.setUserId(feedback.getUser() != null ? feedback.getUser().getId() : null);
        response.setRating(feedback.getRating());
        response.setComment(feedback.getComment());
        response.setSoundQuality(feedback.getSoundQuality());
        response.setVenueExperience(feedback.getVenueExperience());
        response.setArtistPerformance(feedback.getArtistPerformance());
        return response;
    }
}
