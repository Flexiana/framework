#!/bin/bash
docker-compose -f docker-compose.yml up postgres --no-start
docker-compose -f docker-compose.yml start postgres
docker ps
