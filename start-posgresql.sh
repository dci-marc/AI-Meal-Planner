#!/bin/bash

SWD="$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")"

docker compose -f "${SWD}/docker-compose-postgresql.yml" up
