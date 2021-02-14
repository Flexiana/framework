DO $$
BEGIN
   IF exists(SELECT datname FROM pg_database WHERE datname = 'controllers') THEN
      RAISE NOTICE 'Database already exists';
   ELSE
      CREATE DATABASE controllers;
      END IF;
END $$;
GRANT ALL PRIVILEGES ON DATABASE controllers TO postgres;
