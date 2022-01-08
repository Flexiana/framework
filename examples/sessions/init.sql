CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

--;;

CREATE TABLE sessions (
    session_data json not null,
    session_id uuid primary key,
    modified_at timestamp DEFAULT CURRENT_TIMESTAMP
);
