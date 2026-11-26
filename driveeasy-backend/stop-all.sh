#!/usr/bin/env bash
pkill -f "service-registry|config-server|vehicule-service|client-service|location-service|api-gateway" || true
docker compose down || true
