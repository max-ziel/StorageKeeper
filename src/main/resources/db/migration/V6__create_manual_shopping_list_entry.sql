-- Handwritten shopping list entries ("Einkaufsliste"). Not linked to a pantry item.
-- amount and unit are optional, but only together.
CREATE TABLE manual_shopping_list_entry
(
    id       UUID PRIMARY KEY,
    name     VARCHAR(100)             NOT NULL,
    amount   NUMERIC CHECK (amount > 0),
    unit     VARCHAR(20),
    added_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CHECK ((amount IS NULL) = (unit IS NULL))
);