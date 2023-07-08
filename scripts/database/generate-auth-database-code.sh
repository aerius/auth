#!/usr/bin/env bash
set -e

# Change current directory to source directory so it can be called from everywhere
SCRIPT_PATH=$(readlink -f "${0}")
SCRIPT_DIR=$(dirname "${SCRIPT_PATH}")

cd "${SCRIPT_DIR}/../../source"

mvn clean generate-sources -Dgenerate-jooq.url=jdbc:postgresql://localhost:5432/auth
