-- Minimum stock per item, in the item's unit. 0 = no minimum; existing items get 0
ALTER TABLE pantry_item
    ADD COLUMN minimum_amount NUMERIC NOT NULL DEFAULT 0 CHECK ( minimum_amount >= 0 );

-- Speeds up filtering the item list by storage location
CREATE INDEX idx_pantry_item_storage_location_id ON pantry_item (storage_location_id);
