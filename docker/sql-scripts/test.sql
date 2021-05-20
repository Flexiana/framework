

--;;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

--;;
DROP TABLE IF EXISTS
  users
CASCADE;

--;;
CREATE TABLE users
  (
    id         uuid        DEFAULT uuid_generate_v4() PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT now(),
    last_login timestamptz,
    is_active  boolean,
    email      varchar(254) NOT NULL,
    role       varchar(254),
    username   text NOT NULL,
    password   text,
    salt       text,
    fullname   text
  );

--;;
INSERT INTO users (id, created_at, email, role, password, username, is_active)
VALUES ('fd5e0d70-506a-45cc-84d5-b12b5e3e99d2', '2021-03-30 12:34:10.358157+02', 'admin@frankie.sw', 'admin',
        '$2a$11$ivfRMKD7dHMfqCWBiEQcaOknsJgDnK9zoSP/cXAVNQVYHc.M9SZJK', 'admin', true),
       ('31c2c58f-28cb-4013-8765-9240626a18a2', '2021-03-30 12:34:10.358157+02', 'frankie@frankie.sw', 'user',
       '$2a$11$ivf2RMKD7dHMfqCWBiEQcaOknsJgDnK9zoSP/cXAVNQVYHc.M9SZJK', 'frankie', true),
       ('8d05b2e1-6463-478a-ba30-35768738af29', '2021-03-30 12:34:10.358157+02', 'impostor@frankie.sw', 'interviewer',
       '$2a$11$ivfRMKD7dHMfqCWBiEQcaOknsJgDnK9zoSP/cXAVNQVYHc.M9SZJK', 'impostor', false);
