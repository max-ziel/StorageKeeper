-- Speeds up the "expiring soon" query (expiry_date <= ? ORDER BY expiry_date).
CREATE INDEX idx_pantry_item_expiry_date ON pantry_item (expiry_date);