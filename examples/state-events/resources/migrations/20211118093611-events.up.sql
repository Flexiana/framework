CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

--;;

create table EVENTS (
    payload json not null,
    resource varchar,
    resource_id uuid,
    modified_at timestamp,
    action varchar,
    creator uuid
);