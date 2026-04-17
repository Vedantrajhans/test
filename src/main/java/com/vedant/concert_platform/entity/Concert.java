package com.vedant.concert_platform.entity;

import com.vedant.concert_platform.entity.enums.Status;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "concerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Concert extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private Organizer organizer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String genre;

    private LocalDateTime dateTime;
    private LocalDateTime endTime;

    private Integer totalCapacity;
    private Integer ticketsSold = 0;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    private String imageUrl;

    private LocalDateTime ticketSaleStart;
    private LocalDateTime ticketSaleEnd;

    @Version
    private Integer version;
    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public Organizer getOrganizer() { return this.organizer; }
    public void setOrganizer(Organizer organizer) { this.organizer = organizer; }
    public Venue getVenue() { return this.venue; }
    public void setVenue(Venue venue) { this.venue = venue; }
    public String getTitle() { return this.title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return this.description; }
    public void setDescription(String description) { this.description = description; }
    public String getGenre() { return this.genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public LocalDateTime getDateTime() { return this.dateTime; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }
    public LocalDateTime getEndTime() { return this.endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public Integer getTotalCapacity() { return this.totalCapacity; }
    public void setTotalCapacity(Integer totalCapacity) { this.totalCapacity = totalCapacity; }
    public Integer getTicketsSold() { return this.ticketsSold; }
    public void setTicketsSold(Integer ticketsSold) { this.ticketsSold = ticketsSold; }
    public Status getStatus() { return this.status; }
    public void setStatus(Status status) { this.status = status; }
    public String getImageUrl() { return this.imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public LocalDateTime getTicketSaleStart() { return this.ticketSaleStart; }
    public void setTicketSaleStart(LocalDateTime ticketSaleStart) { this.ticketSaleStart = ticketSaleStart; }
    public LocalDateTime getTicketSaleEnd() { return this.ticketSaleEnd; }
    public void setTicketSaleEnd(LocalDateTime ticketSaleEnd) { this.ticketSaleEnd = ticketSaleEnd; }
    public Integer getVersion() { return this.version; }
    public void setVersion(Integer version) { this.version = version; }
}