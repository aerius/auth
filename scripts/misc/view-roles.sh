#!/bin/bash

# Database details
DBNAME="aerius_authorization"
DBUSER="aerius"
DBPASS="aerius"
DBHOST="localhost"  # Assuming your database is on the same machine. Change if needed.

export PGPASSWORD=$DBPASS

# SQL Command to list roles from the auth.roles table
SQL_COMMAND="SELECT * FROM auth.roles;"

# Execute SQL Command
psql -h $DBHOST -U $DBUSER -d $DBNAME -c "$SQL_COMMAND"

# Unset the PGPASSWORD environment variable
unset PGPASSWORD
