FROM postgres:11.5-alpine
COPY init.sql /docker-entrypoint-initdb.d/