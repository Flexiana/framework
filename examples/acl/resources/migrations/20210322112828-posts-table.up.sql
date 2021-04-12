CREATE TABLE posts (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id uuid NOT NULL,
    creation_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    content character varying NOT NULL
);