-- Households: every item, storage location and manual shopping list entry belongs to exactly one.
CREATE TABLE household
(
    id          UUID PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    invite_code VARCHAR(9)  NOT NULL UNIQUE
);

-- user_id is the primary key: a user is a member of at most one household.
CREATE TABLE household_member
(
    user_id      VARCHAR(255) PRIMARY KEY,
    household_id UUID         NOT NULL REFERENCES household (id) ON DELETE CASCADE,
    name         VARCHAR(255) NOT NULL,
    -- Order in which the members joined.
    position     INTEGER      NOT NULL,
    UNIQUE (household_id, position)
);

-- Data from before M5 goes to one household without members. Whoever should get it joins with its invite
-- code, which is random and can be read from the database:  SELECT invite_code FROM household;
-- On an empty database (nothing but the old base location) no such household is created.
INSERT INTO household (id, name, invite_code)
SELECT '00000000-0000-0000-0000-000000000001',
       'Haushalt',
       substr(code, 1, 4) || '-' || substr(code, 5, 4)
FROM (SELECT string_agg(substr('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 1 + floor(random() * 32)::int, 1), '') AS code
      FROM generate_series(1, 8)) AS random_code
WHERE EXISTS (SELECT 1 FROM pantry_item)
   OR EXISTS (SELECT 1 FROM manual_shopping_list_entry)
   OR EXISTS (SELECT 1 FROM storage_location WHERE id <> '00000000-0000-0000-0000-000000000001');

DELETE FROM storage_location WHERE NOT EXISTS (SELECT 1 FROM household);

-- Storage locations: per household, with exactly one default location each.
ALTER TABLE storage_location
    ADD COLUMN household_id     UUID REFERENCES household (id) ON DELETE CASCADE,
    ADD COLUMN default_location BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE storage_location SET household_id = '00000000-0000-0000-0000-000000000001';
UPDATE storage_location SET default_location = TRUE WHERE id = '00000000-0000-0000-0000-000000000001';
ALTER TABLE storage_location
    ALTER COLUMN household_id SET NOT NULL,
    ALTER COLUMN default_location DROP DEFAULT,
    -- Target for the foreign key below: an item can only lie in a location of its own household.
    ADD UNIQUE (id, household_id);

-- Names are unique per household only ("Keller" in two households is fine).
DROP INDEX uq_storage_location_name;
CREATE UNIQUE INDEX uq_storage_location_household_name ON storage_location (household_id, LOWER(name));
CREATE UNIQUE INDEX uq_storage_location_household_default ON storage_location (household_id) WHERE default_location;

-- Pantry items
ALTER TABLE pantry_item ADD COLUMN household_id UUID REFERENCES household (id) ON DELETE CASCADE;
UPDATE pantry_item SET household_id = '00000000-0000-0000-0000-000000000001';
ALTER TABLE pantry_item
    ALTER COLUMN household_id SET NOT NULL,
    ADD FOREIGN KEY (storage_location_id, household_id) REFERENCES storage_location (id, household_id);
CREATE INDEX idx_pantry_item_household_id ON pantry_item (household_id);

-- Manual shopping list entries (stock purchases belong to the household of their item)
ALTER TABLE manual_shopping_list_entry ADD COLUMN household_id UUID REFERENCES household (id) ON DELETE CASCADE;
UPDATE manual_shopping_list_entry SET household_id = '00000000-0000-0000-0000-000000000001';
ALTER TABLE manual_shopping_list_entry ALTER COLUMN household_id SET NOT NULL;
CREATE INDEX idx_manual_shopping_list_entry_household_id ON manual_shopping_list_entry (household_id);
