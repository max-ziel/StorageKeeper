CREATE TABLE pantry_item
(
    id          UUID PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    amount      NUMERIC      NOT NULL CHECK (amount >= 0),
    unit        VARCHAR(20)  NOT NULL,
    expiry_date DATE
);