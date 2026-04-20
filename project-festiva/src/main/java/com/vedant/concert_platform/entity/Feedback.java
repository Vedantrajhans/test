package com.vedant.concert_platform.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "feedback")
public class Feedback extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    private Integer soundQuality;
    private Integer venueExperience;
    private Integer artistPerformance;

    @Version
    private Integer version;

    public Feedback() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Concert getConcert() { return concert; }
    public void setConcert(Concert concert) { this.concert = concert; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Integer getSoundQuality() { return soundQuality; }
    public void setSoundQuality(Integer soundQuality) { this.soundQuality = soundQuality; }
    public Integer getVenueExperience() { return venueExperience; }
    public void setVenueExperience(Integer venueExperience) { this.venueExperience = venueExperience; }
    public Integer getArtistPerformance() { return artistPerformance; }
    public void setArtistPerformance(Integer artistPerformance) { this.artistPerformance = artistPerformance; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
