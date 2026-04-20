-- V7: Performance indexes and schema improvements

-- Faster booking lookups by user+concert
CREATE INDEX IF NOT EXISTS idx_ticket_bookings_user_concert
    ON ticket_bookings(user_id, concert_id);

-- Faster feedback lookups by user+concert
CREATE INDEX IF NOT EXISTS idx_feedback_user_concert
    ON feedback(user_id, concert_id);

-- Index on booking status for filtering
CREATE INDEX IF NOT EXISTS idx_ticket_bookings_status
    ON ticket_bookings(booking_status);

-- Index on concert date for sorting/filtering
CREATE INDEX IF NOT EXISTS idx_concerts_date_time
    ON concerts(date_time);
