-- V1__Create_initial_schema.sql
-- Initial schema for Concert Management System

-- ========================================
-- 1. Users Table (Base for all roles)
-- ========================================
CREATE TABLE users (
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
    
    email               VARCHAR(255) UNIQUE NOT NULL,
    password            VARCHAR(255) NOT NULL,
    first_name          VARCHAR(100),
    last_name           VARCHAR(100),
    phone               VARCHAR(20),
    
    role                VARCHAR(30) NOT NULL CHECK (role IN ('PRODUCER', 'PROMOTER', 'ORGANIZER', 'ATTENDEE')),
    status              VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'PENDING', 'INACTIVE', 'SUSPENDED')),
    
    mfa_enabled         BOOLEAN DEFAULT FALSE,
    mfa_secret          VARCHAR(255),
    
    first_login         BOOLEAN DEFAULT TRUE,           -- Force password setup for organizers
    last_login_at       TIMESTAMP,
    
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by          BIGINT,
    updated_by          BIGINT
);

-- ========================================
-- 2. Organizers Table
-- ========================================
CREATE TABLE organizers (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    
    organizer_type      VARCHAR(50),                     -- Concert Promoter, Festival Organizer, etc.
    preferred_genres    TEXT[],                          -- Array of genres
    company_name        VARCHAR(255),
    address             TEXT,
    city                VARCHAR(100),
    state               VARCHAR(100),
    notes               TEXT,
    verified            BOOLEAN DEFAULT FALSE,
    
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========================================
-- 3. Venues
-- ========================================
CREATE TABLE venues (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    address             TEXT NOT NULL,
    city                VARCHAR(100),
    capacity            INTEGER NOT NULL CHECK (capacity > 0),
    contact_info        TEXT
);

-- ========================================
-- 4. Concerts (Main Entity)
-- ========================================
CREATE TABLE concerts (
    id                  BIGSERIAL PRIMARY KEY,
    organizer_id        BIGINT NOT NULL REFERENCES organizers(id) ON DELETE CASCADE,
    venue_id            BIGINT REFERENCES venues(id),
    
    title               VARCHAR(255) NOT NULL,
    description         TEXT,
    genre               VARCHAR(100),
    date_time           TIMESTAMP NOT NULL,
    end_time            TIMESTAMP,
    
    total_capacity      INTEGER NOT NULL CHECK (total_capacity > 0),
    tickets_sold        INTEGER DEFAULT 0 CHECK (tickets_sold >= 0),
    
    status              VARCHAR(30) DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED', 'LIVE', 'COMPLETED', 'CANCELLED')),
    
    image_url           VARCHAR(500),
    ticket_sale_start   TIMESTAMP,
    ticket_sale_end     TIMESTAMP,
    
    version             INTEGER DEFAULT 0,               -- For optimistic locking
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========================================
-- 5. Ticket Types
-- ========================================
CREATE TABLE ticket_types (
    id                  BIGSERIAL PRIMARY KEY,
    concert_id          BIGINT NOT NULL REFERENCES concerts(id) ON DELETE CASCADE,
    
    name                VARCHAR(100) NOT NULL,           -- VIP, GA, Early Bird, etc.
    price               DECIMAL(10,2) NOT NULL,
    quantity_available  INTEGER NOT NULL CHECK (quantity_available >= 0),
    quantity_sold       INTEGER DEFAULT 0,
    
    description         TEXT
);

-- ========================================
-- 6. Ticket Bookings (Concurrency Critical)
-- ========================================
CREATE TABLE ticket_bookings (
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
    
    user_id             BIGINT NOT NULL REFERENCES users(id),
    concert_id          BIGINT NOT NULL REFERENCES concerts(id),
    ticket_type_id      BIGINT NOT NULL REFERENCES ticket_types(id),
    
    quantity            INTEGER NOT NULL CHECK (quantity > 0),
    total_amount        DECIMAL(12,2) NOT NULL,
    payment_status      VARCHAR(30) DEFAULT 'PENDING',
    booking_status      VARCHAR(30) DEFAULT 'CONFIRMED',
    
    booking_reference   VARCHAR(50) UNIQUE,
    qr_code             TEXT,
    
    version             INTEGER DEFAULT 0,               -- Optimistic locking
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========================================
-- 7. Registrations (for free events)
-- ========================================
CREATE TABLE registrations (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT REFERENCES users(id),
    concert_id          BIGINT NOT NULL REFERENCES concerts(id),
    
    attendee_name       VARCHAR(255),
    email               VARCHAR(255),
    phone               VARCHAR(20),
    status              VARCHAR(30) DEFAULT 'REGISTERED',
    
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========================================
-- 8. Feedback
-- ========================================
CREATE TABLE feedback (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT REFERENCES users(id),
    concert_id          BIGINT NOT NULL REFERENCES concerts(id),
    
    rating              INTEGER CHECK (rating BETWEEN 1 AND 5),
    comment             TEXT,
    sound_quality       INTEGER,
    venue_experience    INTEGER,
    artist_performance  INTEGER,
    
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========================================
-- Indexes for better performance
-- ========================================
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role_status ON users(role, status);
CREATE INDEX idx_organizers_user_id ON organizers(user_id);
CREATE INDEX idx_concerts_organizer ON concerts(organizer_id);
CREATE INDEX idx_concerts_status_date ON concerts(status, date_time);
CREATE INDEX idx_ticket_bookings_user ON ticket_bookings(user_id);
CREATE INDEX idx_feedback_concert ON feedback(concert_id);