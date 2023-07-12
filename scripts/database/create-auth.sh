#!/bin/bash

# Database credentials
DATABASE="aerius_authorization"
USER="aerius"
PASSWORD="aerius"

# Create a new user
echo "Creating user if not exists"
psql -q -U postgres postgres <<OMG
DO \$\$
BEGIN
   IF NOT EXISTS (
      SELECT 1
      FROM pg_catalog.pg_roles
      WHERE  rolname = '$USER')
   THEN
      CREATE ROLE $USER WITH LOGIN PASSWORD '$PASSWORD';
      ALTER ROLE $USER SUPERUSER;
      ALTER USER $USER CREATEDB;
   END IF;
END
\$\$;
OMG

# Check if the database exists
DB_EXISTS=$(psql -U postgres -tAc "SELECT 1 FROM pg_database WHERE datname='$DATABASE'")

# If it doesn't exist, create it
if [ "$DB_EXISTS" = "1" ]
then
    echo "Database $DATABASE already exists. Skipping creation."
else
    echo "Database $DATABASE does not exist. Creating..."
    psql -U postgres -c "CREATE DATABASE $DATABASE WITH OWNER $USER"
fi

echo -e "===\nDatabase creation complete."
