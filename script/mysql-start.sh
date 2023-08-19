#!/bin/bash
docker-compose -f docker-compose.yml up mysql --no-start
docker-compose -f docker-compose.yml start mysql
docker ps