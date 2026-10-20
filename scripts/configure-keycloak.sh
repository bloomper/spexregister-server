#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
set -a
. ./.env
set +a

# Keycloak runs in fgv-infra-deploy's local stack
infra_local=${INFRA_DEPLOY:-../fgv-infra-deploy}/local
admin=$(sed -n 's/^KEYCLOAK_ADMIN=//p' "$infra_local/.env")
web_url=${WEB_URL:-http://localhost:3000}

kcadm() { docker compose -f "$infra_local/compose.yaml" exec -T keycloak-server /opt/keycloak/bin/kcadm.sh "$@"; }
client_uuid() { kcadm get clients -r "$API_KEYCLOAK_REALM" -q clientId="$1" --fields id --format csv --noquotes | tr -d '\r'; }

kcadm config credentials \
  --server http://localhost:8080/auth \
  --realm master \
  --user "$admin" \
  --password "$(cat "$infra_local/.docker/secrets/KEYCLOAK_ADMIN_PASSWORD")"

kcadm update "clients/$(client_uuid "$API_KEYCLOAK_CLIENT_CLIENT_ID")" -r "$API_KEYCLOAK_REALM" \
  -s "redirectUris=[\"$web_url/api/auth/callback/keycloak\",\"https://oauth.pstmn.io/v1/browser-callback\"]"
echo "allowed Postman's callback URL for $API_KEYCLOAK_CLIENT_CLIENT_ID"
