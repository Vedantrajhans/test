package com.vedant.concert_platform.service;

import com.vedant.concert_platform.dto.ConcertDto;
import com.vedant.concert_platform.dto.PageResponse;
import com.vedant.concert_platform.entity.Concert;
import com.vedant.concert_platform.entity.Organizer;
import com.vedant.concert_platform.entity.TicketBooking;
import com.vedant.concert_platform.entity.TicketType;
import com.vedant.concert_platform.entity.User;
import com.vedant.concert_platform.entity.Venue;
import com.vedant.concert_platform.entity.enums.Status;
import com.vedant.concert_platform.exception.BadRequestException;
import com.vedant.concert_platform.exception.ResourceNotFoundException;
import com.vedant.concert_platform.repository.*;
import com.vedant.concert_platform.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertRepository concertRepository;
    private final OrganizerRepository organizerRepository;
    private final UserRepository userRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final VenueRepository venueRepository;
    private final TicketBookingRepository ticketBookingRepository;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public PageResponse<ConcertDto.ConcertResponse> searchConcerts(Status status, String genre,
            LocalDateTime dateFrom, LocalDateTime dateTo, Pageable pageable) {
        Page<Concert> page = concertRepository.searchConcerts(status, genre, dateFrom, dateTo, pageable);
        List<Long> ids = page.stream().map(Concert::getId).toList();
        Map<Long, Concert> hydratedById = concertRepository.findByIdInWithDetails(ids)
                .stream().collect(Collectors.toMap(Concert::getId, c -> c));
        return new PageResponse<>(page.map(c -> mapToResponse(hydratedById.getOrDefault(c.getId(), c))));
    }

    @Transactional(readOnly = true)
    public ConcertDto.ConcertResponse getConcert(Long id) {
        Concert concert = concertRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Concert not found"));
        return mapToResponse(concert);
    }

    @Transactional(readOnly = true)
    public PageResponse<ConcertDto.ConcertResponse> getMyConcerts(Pageable pageable) {
        Organizer organizer = getCurrentOrganizer();
        Page<Concert> page = concertRepository.findByOrganizerId(organizer.getId(), pageable);
        List<Long> ids = page.stream().map(Concert::getId).toList();
        Map<Long, Concert> hydratedById = concertRepository.findByIdInWithDetails(ids)
                .stream().collect(Collectors.toMap(Concert::getId, c -> c));
        return new PageResponse<>(page.map(c -> mapToResponse(hydratedById.getOrDefault(c.getId(), c))));
    }

    @Transactional
    public ConcertDto.ConcertResponse createConcert(ConcertDto.ConcertRequest request) {
        Organizer organizer = getCurrentOrganizer();
        validateDateRelationships(request);

        Concert concert = new Concert();
        concert.setOrganizer(organizer);
        updateConcertState(concert, request);
        concert.setStatus(Status.PENDING);
        Concert savedConcert = concertRepository.save(concert);

        if (request.getTicketTypes() != null) {
            int totalTickets = request.getTicketTypes().stream()
                    .mapToInt(ConcertDto.TicketTypeRequest::getQuantity).sum();
            if (totalTickets > savedConcert.getTotalCapacity()) {
                throw new BadRequestException("Ticket quantity exceeds concert capacity");
            }
            for (ConcertDto.TicketTypeRequest t : request.getTicketTypes()) {
                TicketType ticket = new TicketType();
                ticket.setConcert(savedConcert);
                ticket.setName(t.getName());
                ticket.setPrice(t.getPrice());
                ticket.setQuantityAvailable(t.getQuantity());
                ticket.setQuantitySold(0);
                ticketTypeRepository.save(ticket);
            }
        }
        return mapToResponse(savedConcert);
    }

    @Transactional
    public ConcertDto.ConcertResponse updateConcert(Long id, ConcertDto.ConcertRequest request) {
        Organizer organizer = getCurrentOrganizer();
        Concert concert = concertRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Concert not found"));

        if (!concert.getOrganizer().getId().equals(organizer.getId())) {
            throw new ResourceNotFoundException("Concert not found in your account");
        }

        validateDateRelationships(request);

        if (request.getTotalCapacity() != null && concert.getTicketsSold() != null
                && request.getTotalCapacity() < concert.getTicketsSold()) {
            throw new BadRequestException("Cannot reduce capacity below tickets already sold ("
                    + concert.getTicketsSold() + ")");
        }

        // Detect changes that affect attendees (venue or time change)
        boolean venueChanged = isVenueChanged(concert, request);
        boolean timeChanged = request.getDateTime() != null && !request.getDateTime().equals(concert.getDateTime());
        List<String> changes = new ArrayList<>();
        if (venueChanged) changes.add("Venue has been updated");
        if (timeChanged) changes.add("Concert date/time has been updated to " + request.getDateTime());

        updateConcertState(concert, request);

        if (request.getTicketTypes() != null && !request.getTicketTypes().isEmpty()) {
            List<TicketType> existingTickets = ticketTypeRepository.findByConcertId(concert.getId());
            boolean anySold = existingTickets.stream().anyMatch(t -> t.getQuantitySold() != null && t.getQuantitySold() > 0);
            if (anySold) {
                throw new BadRequestException(
                        "Cannot modify ticket types after tickets have been sold.");
            }
            ticketTypeRepository.deleteAll(existingTickets);
            int totalTickets = request.getTicketTypes().stream()
                    .mapToInt(ConcertDto.TicketTypeRequest::getQuantity).sum();
            if (totalTickets > request.getTotalCapacity()) {
                throw new BadRequestException("Ticket quantity exceeds concert capacity");
            }
            for (ConcertDto.TicketTypeRequest t : request.getTicketTypes()) {
                TicketType ticket = new TicketType();
                ticket.setConcert(concert);
                ticket.setName(t.getName());
                ticket.setPrice(t.getPrice());
                ticket.setQuantityAvailable(t.getQuantity());
                ticket.setQuantitySold(0);
                ticketTypeRepository.save(ticket);
            }
        }

        Concert saved = concertRepository.save(concert);

        // Notify attendees if venue or time changed
        if (!changes.isEmpty()) {
            String changeDesc = String.join(". ", changes) + ".";
            List<TicketBooking> confirmedBookings = ticketBookingRepository.findConfirmedByConcertIdWithDetails(saved.getId());
            if (!confirmedBookings.isEmpty()) {
                emailService.sendConcertUpdateNotification(confirmedBookings, saved, changeDesc);
            }
        }

        return mapToResponse(saved);
    }

    @Transactional
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

    @Transactional
    public void deleteConcert(Long id) {
        Organizer organizer = getCurrentOrganizer();
        Concert concert = concertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Concert not found"));
        if (!concert.getOrganizer().getId().equals(organizer.getId())) {
            throw new ResourceNotFoundException("Concert not found in your account");
        }
        concert.setStatus(Status.CANCELLED);
        concertRepository.save(concert);
    }

    // Fix #12: export attendee CSV for concerts with capacity < 500
    @Transactional(readOnly = true)
    public byte[] exportAttendeeCsv(Long concertId) {
        Organizer organizer = getCurrentOrganizer();
        Concert concert = concertRepository.findByIdWithDetails(concertId)
                .orElseThrow(() -> new ResourceNotFoundException("Concert not found"));
        if (!concert.getOrganizer().getId().equals(organizer.getId())) {
            throw new ResourceNotFoundException("Concert not found in your account");
        }
        if (concert.getTotalCapacity() == null || concert.getTotalCapacity() >= 500) {
            throw new BadRequestException("Attendee export is only available for concerts with capacity less than 500.");
        }
        List<TicketBooking> bookings = ticketBookingRepository.findConfirmedByConcertIdWithDetails(concertId);

        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
             java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(out);
             com.opencsv.CSVWriter csv = new com.opencsv.CSVWriter(writer)) {
            csv.writeNext(new String[]{"Serial", "Name", "Phone", "Email", "Ticket Type", "Quantity"});
            int serial = 1;
            // Sort by ticket type name
            bookings.sort((a, b) -> a.getTicketType().getName().compareTo(b.getTicketType().getName()));
            for (TicketBooking b : bookings) {
                String name = b.getUser().getFirstName() + " " + b.getUser().getLastName();
                csv.writeNext(new String[]{
                        String.valueOf(serial++),
                        name,
                        b.getUser().getPhone() != null ? b.getUser().getPhone() : "",
                        b.getUser().getEmail(),
                        b.getTicketType().getName(),
                        String.valueOf(b.getQuantity())
                });
            }
            csv.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BadRequestException("Failed to export CSV: " + e.getMessage());
        }
    }

    private boolean isVenueChanged(Concert concert, ConcertDto.ConcertRequest request) {
        if (request.getVenueId() != null && concert.getVenue() != null) {
            return !request.getVenueId().equals(concert.getVenue().getId());
        }
        if (request.getVenueCity() != null && concert.getVenue() != null) {
            return !request.getVenueCity().equals(concert.getVenue().getCity());
        }
        if (request.getVenueAddress() != null && concert.getVenue() != null) {
            return !request.getVenueAddress().equals(concert.getVenue().getAddress());
        }
        return false;
    }

    private void validateDateRelationships(ConcertDto.ConcertRequest request) {
        LocalDateTime dateTime = request.getDateTime();
        LocalDateTime endTime = request.getEndTime();
        LocalDateTime saleStart = request.getTicketSaleStart();
        LocalDateTime saleEnd = request.getTicketSaleEnd();

        if (dateTime == null) return;

        if (endTime != null && !endTime.isAfter(dateTime)) {
            throw new BadRequestException("End time must be after start time");
        }
        // Fix #7: saleEnd must be <= concertStart (not strictly before), allow saleEnd = concertStart
        if (saleEnd != null && saleEnd.isAfter(dateTime)) {
            throw new BadRequestException("Ticket sale end must be on or before concert start time");
        }
        if (saleStart != null && saleEnd != null && !saleEnd.isAfter(saleStart)) {
            throw new BadRequestException("Ticket sale end must be after ticket sale start");
        }
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

        if (request.getVenueId() != null) {
            Venue venue = venueRepository.findById(request.getVenueId())
                    .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));
            concert.setVenue(venue);
        } else if (request.getVenueAddress() != null || request.getVenueCity() != null) {
            Venue venue = concert.getVenue() != null ? concert.getVenue() : new Venue();
            if (request.getVenueAddress() != null) venue.setAddress(request.getVenueAddress());
            if (request.getVenueCity() != null) venue.setCity(request.getVenueCity());
            venue = venueRepository.save(venue);
            concert.setVenue(venue);
        }
    }

    private ConcertDto.ConcertResponse mapToResponse(Concert concert) {
        ConcertDto.ConcertResponse response = new ConcertDto.ConcertResponse();
        response.setId(concert.getId());
        response.setTitle(concert.getTitle());
        response.setDescription(concert.getDescription());
        response.setGenre(concert.getGenre());
        response.setDateTime(concert.getDateTime());
        response.setEndTime(concert.getEndTime());
        response.setOrganizerId(concert.getOrganizer().getId());
        response.setTotalCapacity(concert.getTotalCapacity());
        response.setTicketsSold(concert.getTicketsSold());
        response.setStatus(concert.getStatus());
        response.setTicketSaleStart(concert.getTicketSaleStart());
        response.setTicketSaleEnd(concert.getTicketSaleEnd());
        response.setImageUrl(concert.getImageUrl());

        if (concert.getVenue() != null) {
            response.setVenueId(concert.getVenue().getId());
            response.setVenueAddress(concert.getVenue().getAddress());
            response.setVenueCity(concert.getVenue().getCity());
        }

        List<TicketType> tickets = ticketTypeRepository.findByConcertId(concert.getId());
        response.setTicketTypes(tickets.stream().map(t -> {
            ConcertDto.TicketTypeResponse dto = new ConcertDto.TicketTypeResponse();
            dto.setId(t.getId());
            dto.setName(t.getName());
            dto.setPrice(t.getPrice());
            dto.setAvailableQuantity(t.getQuantityAvailable());
            dto.setSoldQuantity(t.getQuantitySold());
            return dto;
        }).toList());
        return response;
    }
}
