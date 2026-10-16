#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
set -a
. ./.env
set +a

web_url=${WEB_URL:-http://localhost:3000}

kcadm() { docker compose exec -T auth /opt/keycloak/bin/kcadm.sh "$@"; }
client_uuid() { kcadm get clients -r "$API_AUTH_REALM" -q clientId="$1" --fields id --format csv --noquotes | tr -d '\r'; }

kcadm config credentials \
  --server http://localhost:8081/auth \
  --realm master \
  --user "$AUTH_ADMIN" \
  --password "$(cat .docker/secrets/AUTH_ADMIN_PASSWORD)"

kcadm update "clients/$(client_uuid "$API_AUTH_CLIENT_CLIENT_ID")" -r "$API_AUTH_REALM" \
  -s "redirectUris=[\"$web_url/api/auth/callback/keycloak\",\"https://oauth.pstmn.io/v1/browser-callback\"]"
echo "allowed Postman's callback URL for $API_AUTH_CLIENT_CLIENT_ID"
