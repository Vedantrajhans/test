-- V2__Add_artists_and_concert_artists.sql

CREATE TABLE artists (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    genre               VARCHAR(255),
    bio                 TEXT
);

CREATE TABLE concert_artists (
    concert_id          BIGINT NOT NULL REFERENCES concerts(id) ON DELETE CASCADE,
    artist_id           BIGINT NOT NULL REFERENCES artists(id) ON DELETE CASCADE,
    performance_order   INTEGER,
    PRIMARY KEY (concert_id, artist_id)
);

CREATE INDEX idx_concert_artists_concert ON concert_artists(concert_id);
CREATE INDEX idx_concert_artists_artist ON concert_artists(artist_id);
