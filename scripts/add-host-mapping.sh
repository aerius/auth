#!/bin/bash
HOSTS_FILE="/etc/hosts"
HOST_LINE="127.0.0.1   aerius-auth-local"

# Check if the line already exists in the file
if ! grep -q "$HOST_LINE" $HOSTS_FILE; then
    # If it doesn't exist, append it to the file
    echo "$HOST_LINE" | sudo tee -a $HOSTS_FILE > /dev/null
    echo "Added $HOST_LINE to your $HOSTS_FILE"
else
    echo "$HOST_LINE already exists in your $HOSTS_FILE"
fi
