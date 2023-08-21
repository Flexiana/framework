-- Drop the database if it exists
DROP DATABASE IF EXISTS framework;

-- Create the database
CREATE DATABASE framework;

-- Grant privileges to a MySQL user
GRANT ALL PRIVILEGES ON framework.* TO 'username'@'localhost';

-- Create UUID function
CREATE FUNCTION uuid()
RETURNS CHAR(36)
BEGIN
    RETURN LOWER(REPLACE(UUID(), '-', ''));
END;
