CREATE TABLE txoutbox (
    id VARCHAR(36) PRIMARY KEY,
    invocation TEXT,
    nextAttemptTime TIMESTAMP,
    attempts INT,
    lastAttemptTime TIMESTAMP,
    blocked BOOLEAN,
    version INT,
    uniqueName VARCHAR(250)
);

CREATE INDEX idx_txoutbox_nextAttemptTime ON txoutbox (nextAttemptTime);
