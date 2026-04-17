package com.vedant.concert_platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ticket_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Each ticket type belongs to a concert
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    private String name; // VIP, GA, Early Bird

    private BigDecimal price;

    private Integer quantityAvailable;

    private Integer quantitySold = 0;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Version
    private Integer version;
    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public Concert getConcert() { return this.concert; }
    public void setConcert(Concert concert) { this.concert = concert; }
    public String getName() { return this.name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPrice() { return this.price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getQuantityAvailable() { return this.quantityAvailable; }
    public void setQuantityAvailable(Integer quantityAvailable) { this.quantityAvailable = quantityAvailable; }
    public Integer getQuantitySold() { return this.quantitySold; }
    public void setQuantitySold(Integer quantitySold) { this.quantitySold = quantitySold; }
    public String getDescription() { return this.description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getVersion() { return this.version; }
    public void setVersion(Integer version) { this.version = version; }
}
