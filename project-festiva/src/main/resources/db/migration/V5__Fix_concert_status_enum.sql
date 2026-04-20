-- V5__Fix_concert_status_enum.sql
-- Fixes the status CHECK constraint for concerts table to match Java Status enum

-- Drop the old constraint if it exists
ALTER TABLE concerts DROP CONSTRAINT IF EXISTS concerts_status_check;

-- Add a comprehensive constraint that supports both your current Java enum and common concert states
ALTER TABLE concerts 
ADD CONSTRAINT concerts_status_check 
CHECK (status IN ('ACTIVE', 'PENDING', 'INACTIVE', 'SUSPENDED', 'DRAFT', 'PUBLISHED', 'LIVE', 'COMPLETED', 'CANCELLED'));

-- Update default to 'PENDING' to match your ConcertService logic
ALTER TABLE concerts ALTER COLUMN status SET DEFAULT 'PENDING';

-- Optional: Update any existing rows that might have invalid status (safety step)
UPDATE concerts 
SET status = 'PENDING' 
WHERE status NOT IN ('ACTIVE', 'PENDING', 'INACTIVE', 'SUSPENDED', 'DRAFT', 'PUBLISHED', 'LIVE', 'COMPLETED', 'CANCELLED');