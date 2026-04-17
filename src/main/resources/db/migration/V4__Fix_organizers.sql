-- V4__Fix_organizers.sql
CREATE TABLE organizer_genres (
    organizer_id BIGINT NOT NULL REFERENCES organizers(id) ON DELETE CASCADE,
    genre VARCHAR(255)
);

CREATE INDEX idx_organizer_genres_organizer ON organizer_genres(organizer_id);

ALTER TABLE organizers ADD COLUMN status VARCHAR(50);

ALTER TABLE organizers DROP COLUMN preferred_genres;
