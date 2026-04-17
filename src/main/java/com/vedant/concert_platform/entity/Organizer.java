package com.vedant.concert_platform.entity;

import com.vedant.concert_platform.entity.enums.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "organizers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organizer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promoter_id", nullable = false)
    private Promoter promoter;

    @NotBlank
    private String organizerType;           // e.g., "Concert Promoter", "Festival Organizer", "Independent Gig"

    @ElementCollection
    @CollectionTable(name = "organizer_genres", joinColumns = @JoinColumn(name = "organizer_id"))
    @Column(name = "genre")
    private List<String> preferredGenres;   // e.g., ["Rock", "EDM", "Bollywood"]

    private String companyName;
    private String address;
    private String city;
    private String state;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private boolean verified = false;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;
    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return this.user; }
    public void setUser(User user) { this.user = user; }
    public Promoter getPromoter() { return this.promoter; }
    public void setPromoter(Promoter promoter) { this.promoter = promoter; }
    public String getOrganizerType() { return this.organizerType; }
    public void setOrganizerType(String organizerType) { this.organizerType = organizerType; }
    public List<String> getPreferredGenres() { return this.preferredGenres; }
    public void setPreferredGenres(List<String> preferredGenres) { this.preferredGenres = preferredGenres; }
    public String getCompanyName() { return this.companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getAddress() { return this.address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return this.city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return this.state; }
    public void setState(String state) { this.state = state; }
    public String getNotes() { return this.notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public boolean getVerified() { return this.verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    public Status getStatus() { return this.status; }
    public void setStatus(Status status) { this.status = status; }
}
