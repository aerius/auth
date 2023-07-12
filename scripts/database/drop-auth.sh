#!/bin/bash

# Database credentials
DATABASE="aerius_authorization"

dropdb -U postgres $DATABASE

echo -e "===\nDatabase uncreation complete."
