#!/bin/sh
set -eu

CONNECT_URL="${CONNECT_URL:-http://connect:8083}"

until curl -sf "${CONNECT_URL}/connectors" >/dev/null 2>&1; do
  echo "Waiting for Kafka Connect at ${CONNECT_URL}..."
  sleep 2
done

for config in /config/*-connector.json; do
  [ -e "$config" ] || continue
  name=$(basename "$config" -connector.json)
  echo "Registering connector ${name}"
  curl -sS -X PUT \
    -H "Content-Type: application/json" \
    --data @"$config" \
    "${CONNECT_URL}/connectors/${name}/config"
  echo
done
