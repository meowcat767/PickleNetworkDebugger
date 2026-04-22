#!/bin/bash

# This script helps to grant the necessary capabilities to the Java binary
# to allow raw packet capture without running the entire application as root.

JAVA_PATH=$(readlink -f $(which java))

echo "Setting capabilities for: $JAVA_PATH"

if sudo setcap cap_net_raw,cap_net_admin=eip "$JAVA_PATH"; then
    echo "Successfully granted CAP_NET_RAW and CAP_NET_ADMIN to $JAVA_PATH"
    echo "You can now run the application without sudo."
else
    echo "Failed to set capabilities. Make sure you have sudo privileges."
    exit 1
fi
