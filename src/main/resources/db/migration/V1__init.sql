CREATE TABLE urls (
    id UUID PRIMARY KEY,
    original_url TEXT NOT NULL UNIQUE,
    short_code VARCHAR(255) NOT NULL UNIQUE
);

CREATE INDEX idx_urls_short_code ON urls(short_code);
CREATE INDEX idx_urls_original_url ON urls(original_url);
