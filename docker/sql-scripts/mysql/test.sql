-- Create UUID function
DELIMITER //
CREATE FUNCTION uuid()
RETURNS CHAR(36)
BEGIN
    RETURN LOWER(REPLACE(UUID(), '-', ''));
END;
//
DELIMITER ;

-- Drop the table if it exists
DROP TABLE IF EXISTS users;

-- Create the table
CREATE TABLE users
(
    id         CHAR(36)    DEFAULT uuid() PRIMARY KEY,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login DATETIME,
    is_active  BOOLEAN,
    email      VARCHAR(254) NOT NULL,
    role       VARCHAR(254),
    username   TEXT NOT NULL,
    password   TEXT,
    salt       TEXT,
    fullname   TEXT
);

-- Insert data into the table
INSERT INTO users (id, created_at, email, role, password, username, is_active)
VALUES
    ('fd5e0d70-506a-45cc-84d5-b12b5e3e99d2', '2021-03-30 12:34:10', 'admin@frankie.sw', 'admin', '$2a$11$ivfRMKD7dHMfqCWBiEQcaOknsJgDnK9zoSP/cXAVNQVYHc.M9SZJK', 'admin', 1),
    ('31c2c58f-28cb-4013-8765-9240626a18a2', '2021-03-30 12:34:10', 'frankie@frankie.sw', 'user', '$2a$11$ivf2RMKD7dHMfqCWBiEQcaOknsJgDnK9zoSP/cXAVNQVYHc.M9SZJK', 'frankie', 1),
    ('8d05b2e1-6463-478a-ba30-35768738af29', '2021-03-30 12:34:10', 'impostor@frankie.sw', 'interviewer', '$2a$11$ivfRMKD7dHMfqCWBiEQcaOknsJgDnK9zoSP/cXAVNQVYHc.M9SZJK', 'impostor', 0);
