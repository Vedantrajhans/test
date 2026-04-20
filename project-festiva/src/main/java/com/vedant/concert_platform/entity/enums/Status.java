package com.vedant.concert_platform.entity.enums;

public enum Status {
    // Common for both users/organizers and concerts
    ACTIVE,
    PENDING,
    INACTIVE,
    SUSPENDED,

    // Concert-specific
    DRAFT,
    PUBLISHED,
    LIVE,
    COMPLETED,
    CANCELLED
}