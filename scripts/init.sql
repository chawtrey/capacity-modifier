CREATE TABLE IF NOT EXISTS opp_records
(
    id   SERIAL PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    date DATE        NOT NULL
)
