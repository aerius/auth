#!/bin/bash

# First, you need to ensure that you have received all the necessary arguments
if [ "$#" -ne 3 ]; then
    echo "You must enter exactly 3 arguments: username, plaintext password, and role name"
    exit 1
fi

# Assigning command line arguments to variables
DBNAME="aerius_authorization"
DBUSER="aerius"
DBPASS="aerius"
DBHOST="localhost"

USERNAME=$1
PLAIN_PASSWORD=$2
ROLE=$3

# Create a temporary file for htpasswd output
TEMP_FILE=$(mktemp)

# Generate bcrypt hash with htpasswd
# Here, -B means bcrypt, -bn means don't add a newline and username:password is the input
htpasswd -Bbn "$USERNAME" "$PLAIN_PASSWORD" > "$TEMP_FILE"

# Extract just the hash from the output
# Cut -d: -f2 removes the username and colon at the start, and rev | cut -c4- | rev removes the last three characters
BCRYPT_HASH=$(cut -d: -f2 "$TEMP_FILE")

# Clean up the temporary file
rm "$TEMP_FILE"

export PGPASSWORD=$DBPASS

# SQL Command
SQL_COMMAND="SELECT auth.ae_create_local_user('$USERNAME', '$BCRYPT_HASH', '$ROLE');"

# Execute SQL Command
psql -h $DBHOST -U $DBUSER -d $DBNAME -c "$SQL_COMMAND"

# Unset the PGPASSWORD environment variable
unset PGPASSWORD
