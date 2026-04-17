package com.vedant.concert_platform.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "producers")
@Data
public class Producer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;
}
