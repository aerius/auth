#!/usr/bin/env bash

# Exit on error
set -e

# Change current directory to directory of script so it can be called from everywhere
SCRIPT_PATH=$(readlink -f "${0}")
SCRIPT_DIR=$(dirname "${SCRIPT_PATH}")
cd "${SCRIPT_DIR}"

# Build like we are in the root directory
docker-compose --project-directory ../ --env-file docker/.env build
