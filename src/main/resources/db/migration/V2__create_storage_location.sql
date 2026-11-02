CREATE TABLE storage_location
(
    id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL
);

-- Names are unique regardless of case ("Keller" = "keller")
CREATE UNIQUE INDEX uq_storage_location_name ON storage_location (LOWER(name));

-- Base location; its id is StorageLocationId.Default in the code.
INSERT INTO storage_location (id, name) VALUES ('00000000-0000-0000-0000-000000000001', 'Vorrat');

-- Every item needs a location; existing items are moved to the base location.
ALTER TABLE pantry_item
    ADD COLUMN storage_location_id UUID NOT NULL
        DEFAULT '00000000-0000-0000-0000-000000000001'
        REFERENCES storage_location (id);

ALTER TABLE pantry_item
    ALTER COLUMN storage_location_id DROP DEFAULT;
