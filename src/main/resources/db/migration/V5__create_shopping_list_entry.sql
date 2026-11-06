-- At most one entry per item; deleting an item deletes its entry as well.
CREATE TABLE shopping_list_entry
(
    id UUID primary key,
    pantry_item_id UUID NOT NULL UNIQUE REFERENCES pantry_item (id) ON DELETE CASCADE,
    added_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Items that are already below their minimum stock start out on the list.
INSERT INTO shopping_list_entry (id, pantry_item_id, added_at)
SELECT gen_random_uuid(), id, now()
FROM pantry_item
WHERE amount < minimum_amount;
