package com.vedant.concert_platform.service;

import com.vedant.concert_platform.dto.ConcertDto;
import com.vedant.concert_platform.dto.PageResponse;
import com.vedant.concert_platform.entity.Concert;
import com.vedant.concert_platform.entity.Organizer;
import com.vedant.concert_platform.entity.User;
import com.vedant.concert_platform.entity.enums.Status;
import com.vedant.concert_platform.exception.BadRequestException;
import com.vedant.concert_platform.exception.ResourceNotFoundException;
import com.vedant.concert_platform.repository.ConcertRepository;
import com.vedant.concert_platform.repository.OrganizerRepository;
import com.vedant.concert_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertRepository concertRepository;
    private final OrganizerRepository organizerRepository;
    private final UserRepository userRepository;

    public PageResponse<ConcertDto.ConcertResponse> searchConcerts(Status status, String genre, LocalDateTime dateFrom, LocalDateTime dateTo, Pageable pageable) {
        Page<Concert> concerts = concertRepository.searchConcerts(status, genre, dateFrom, dateTo, pageable);
        return new PageResponse<>(concerts.map(this::mapToResponse));
    }

    public ConcertDto.ConcertResponse getConcert(Long id) {
        Concert concert = concertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Concert not found"));
        return mapToResponse(concert);
    }

    public PageResponse<ConcertDto.ConcertResponse> getMyConcerts(Pageable pageable) {
        Organizer organizer = getCurrentOrganizer();
        Page<Concert> concerts = concertRepository.findByOrganizerId(organizer.getId(), pageable);
        return new PageResponse<>(concerts.map(this::mapToResponse));
    }

    public ConcertDto.ConcertResponse createConcert(ConcertDto.ConcertRequest request) {
        Organizer organizer = getCurrentOrganizer();
        
        Concert concert = new Concert();
        concert.setOrganizer(organizer);
        updateConcertState(concert, request);
        concert.setStatus(Status.PENDING); // initially drafts/pending
        
        return mapToResponse(concertRepository.save(concert));
    }

    public ConcertDto.ConcertResponse updateConcert(Long id, ConcertDto.ConcertRequest request) {
        Organizer organizer = getCurrentOrganizer();
        Concert concert = concertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Concert not found"));

        if (!concert.getOrganizer().getId().equals(organizer.getId())) {
            throw new ResourceNotFoundException("Concert not found in your account");
        }

        updateConcertState(concert, request);
        return mapToResponse(concertRepository.save(concert));
    }

    public void publishConcert(Long id) {
        Organizer organizer = getCurrentOrganizer();
        Concert concert = concertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Concert not found"));

        if (!concert.getOrganizer().getId().equals(organizer.getId())) {
            throw new ResourceNotFoundException("Concert not found in your account");
        }

        concert.setStatus(Status.ACTIVE);
        concertRepository.save(concert);
    }
    
    public void deleteConcert(Long id) {
        Organizer organizer = getCurrentOrganizer();
        Concert concert = concertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Concert not found"));

        if (!concert.getOrganizer().getId().equals(organizer.getId())) {
            throw new ResourceNotFoundException("Concert not found in your account");
        }

        concert.setStatus(Status.INACTIVE);
        concertRepository.save(concert);
    }

    private Organizer getCurrentOrganizer() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(username).orElseThrow();
        if (user.isFirstLogin()) {
            throw new BadRequestException("Complete first-login password setup before managing concerts");
        }
        if (!user.isMfaEnabled()) {
            throw new BadRequestException("MFA is required for organizer actions");
        }
        return organizerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Organizer profile not found"));
    }

    private void updateConcertState(Concert concert, ConcertDto.ConcertRequest request) {
        concert.setTitle(request.getTitle());
        concert.setDescription(request.getDescription());
        concert.setGenre(request.getGenre());
        concert.setDateTime(request.getDateTime());
        concert.setEndTime(request.getEndTime());
        concert.setTotalCapacity(request.getTotalCapacity());
        concert.setTicketSaleStart(request.getTicketSaleStart());
        concert.setTicketSaleEnd(request.getTicketSaleEnd());
        concert.setImageUrl(request.getImageUrl());
        // venue id mapping omitted for brevity, string/objects should be set properly
    }

    private ConcertDto.ConcertResponse mapToResponse(Concert concert) {
        ConcertDto.ConcertResponse response = new ConcertDto.ConcertResponse();
        response.setId(concert.getId());
        response.setTitle(concert.getTitle());
        response.setDescription(concert.getDescription());
        response.setGenre(concert.getGenre());
        response.setDateTime(concert.getDateTime());
        response.setEndTime(concert.getEndTime());
        response.setVenueId(concert.getVenue() != null ? concert.getVenue().getId() : null);
        response.setOrganizerId(concert.getOrganizer().getId());
        response.setTotalCapacity(concert.getTotalCapacity());
        response.setTicketsSold(concert.getTicketsSold());
        response.setStatus(concert.getStatus());
        response.setTicketSaleStart(concert.getTicketSaleStart());
        response.setTicketSaleEnd(concert.getTicketSaleEnd());
        response.setImageUrl(concert.getImageUrl());
        return response;
    }
}
