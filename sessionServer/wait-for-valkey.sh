#!/bin/sh

# Wait for Valkey servers to be ready
echo "Waiting for Valkey servers to be ready..."

# Wait for primary
until valkey-cli -h valkey-session-primary -p 6379 -a valkey_session_password ping > /dev/null 2>&1; do
  echo "Waiting for valkey-session-primary..."
  sleep 2
done

# Wait for replica1
until valkey-cli -h valkey-session-replica1 -p 6379 -a valkey_session_password ping > /dev/null 2>&1; do
  echo "Waiting for valkey-session-replica1..."
  sleep 2
done

# Wait for replica2
until valkey-cli -h valkey-session-replica2 -p 6379 -a valkey_session_password ping > /dev/null 2>&1; do
  echo "Waiting for valkey-session-replica2..."
  sleep 2
done

echo "All Valkey servers are ready. Starting Sentinel..."
exec valkey-sentinel /usr/local/etc/valkey/sentinel.conf
