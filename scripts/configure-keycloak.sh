#!/usr/bin/env bash
# Local development only: sets this application up against the Keycloak of fgv-infra-deploy's local stack. Copies the
# secrets of the clients the API uses (the admin client and, where the application has one, its service client) from
# Keycloak into .docker/secrets, sets the web client's id and secret in the web's .env.local, and allows Postman's
# callback URL. Run scripts/init-secrets.sh for the other secrets. It can be run again.
set -euo pipefail

cd "$(dirname "$0")/.."
set -a
. ./.env
set +a

# Keycloak runs in fgv-infra-deploy's local stack
infra_local=${INFRA_DEPLOY:-../fgv-infra-deploy}/local
admin=$(sed -n 's/^KEYCLOAK_ADMIN=//p' "$infra_local/.env")
web_url=${WEB_URL:-http://localhost:3000}
# The web's checkout next to this one, e.g. fgv-spexgram-web next to fgv-spexgram-server
web_dir=${WEB_DIR:-../$(basename "$PWD" | sed 's/-server$/-web/')}

kcadm() { docker compose -f "$infra_local/compose.yaml" exec -T keycloak-server /opt/keycloak/bin/kcadm.sh "$@"; }
client_uuid() { kcadm get clients -r "$API_KEYCLOAK_REALM" -q clientId="$1" --fields id --format csv --noquotes | tr -d '\r'; }
client_secret() {
  local uuid
  uuid=$(client_uuid "$1")
  if [[ -z $uuid ]]; then
    echo "the realm $API_KEYCLOAK_REALM has no client $1: Keycloak's image is older than this application needs, or the realm was imported before the client was added" >&2
    exit 1
  fi
  kcadm get "clients/$uuid/client-secret" -r "$API_KEYCLOAK_REALM" --fields value --format csv --noquotes | tr -d '\r'
}

# Writes the secret of a client to .docker/secrets
set_secret() {
  local file=.docker/secrets/$1 client=$2 value
  value=$(client_secret "$client")
  if [[ -f $file && "$(cat "$file")" == "$value" ]]; then
    echo "keep $file"
  else
    printf '%s' "$value" > "$file"
    echo "set $file to the secret of $client"
  fi
}

# Sets a variable in the web's .env.local, adding it when missing
set_env() {
  local file=$web_dir/.env.local key=$1 value=$2
  if grep -qxF "$key=$value" "$file"; then
    echo "keep $key in $file"
    return
  fi
  awk -v k="$key" -v v="$value" 'index($0, k "=") == 1 { print k "=" v; done = 1; next } { print } END { if (!done) print k "=" v }' \
    "$file" > "$file.tmp"
  mv "$file.tmp" "$file"
  echo "set $key in $file"
}

kcadm config credentials \
  --server http://localhost:8080/auth \
  --realm master \
  --user "$admin" \
  --password "$(cat "$infra_local/.docker/secrets/KEYCLOAK_ADMIN_PASSWORD")"

set_secret API_KEYCLOAK_ADMIN_CLIENT_SECRET "$API_KEYCLOAK_ADMIN_CLIENT_ID"
if [[ -f .docker/secrets/API_KEYCLOAK_SERVICE_CLIENT_SECRET ]]; then
  set_secret API_KEYCLOAK_SERVICE_CLIENT_SECRET "${API_KEYCLOAK_SERVICE_CLIENT_ID:-$API_KEYCLOAK_CLIENT_CLIENT_ID-service}"
fi
git update-index --skip-worktree .docker/secrets/*

web_secret=$(client_secret "$API_KEYCLOAK_CLIENT_CLIENT_ID")
if [[ -f $web_dir/.env.local ]]; then
  set_env KEYCLOAK_CLIENT_ID "$API_KEYCLOAK_CLIENT_CLIENT_ID"
  set_env KEYCLOAK_CLIENT_SECRET "$web_secret"
else
  echo "no $web_dir/.env.local: create it as the web's README describes, then run this again (WEB_DIR names the web's checkout)"
fi

kcadm update "clients/$(client_uuid "$API_KEYCLOAK_CLIENT_CLIENT_ID")" -r "$API_KEYCLOAK_REALM" \
  -s "redirectUris=[\"$web_url/api/auth/callback/keycloak\",\"https://oauth.pstmn.io/v1/browser-callback\"]"
echo "allowed Postman's callback URL for $API_KEYCLOAK_CLIENT_CLIENT_ID"
