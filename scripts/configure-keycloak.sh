#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
set -a
. ./.env
set +a

web_url=${WEB_URL:-http://localhost:3000}

kcadm() { docker compose exec -T keycloak /opt/keycloak/bin/kcadm.sh "$@"; }
client_uuid() { kcadm get clients -r "$APP_KEYCLOAK_REALM" -q clientId="$1" --fields id --format csv --noquotes | tr -d '\r'; }

kcadm config credentials \
  --server http://localhost:8081/auth \
  --realm master \
  --user "$KEYCLOAK_ADMIN" \
  --password "$(cat .docker/secrets/KEYCLOAK_ADMIN_PASSWORD)"

kcadm update "clients/$(client_uuid "$APP_KEYCLOAK_CLIENT_CLIENT_ID")" -r "$APP_KEYCLOAK_REALM" \
  -s "rootUrl=$web_url" \
  -s "baseUrl=$web_url/" \
  -s "redirectUris=[\"$web_url/api/auth/callback/keycloak\",\"https://oauth.pstmn.io/v1/browser-callback\"]" \
  -s 'webOrigins=[]' \
  -s "attributes.\"post.logout.redirect.uris\"=$web_url##$web_url/"
echo "updated $APP_KEYCLOAK_CLIENT_CLIENT_ID URLs ($web_url)"

mcp_url=${MCP_URL:-https://api.register.fgv.local.nu:8443/api/mcp}
mcp_client_uuid=$(client_uuid "${APP_KEYCLOAK_MCP_CLIENT_ID:-spexregister-mcp}")
mcp_audience=(
  -s "name=mcp audience"
  -s "protocol=openid-connect"
  -s "protocolMapper=oidc-audience-mapper"
  -s "config.\"included.custom.audience\"=$mcp_url"
  -s 'config."access.token.claim"=true'
  -s 'config."id.token.claim"=false'
  -s 'config."introspection.token.claim"=true'
)
mcp_audience_id=$(kcadm get "clients/$mcp_client_uuid/protocol-mappers/models" -r "$APP_KEYCLOAK_REALM" --fields id,name --format csv --noquotes \
  | tr -d '\r' | awk -F, '$2 == "mcp audience" { print $1 }')
if [[ -n "$mcp_audience_id" ]]; then
  kcadm update "clients/$mcp_client_uuid/protocol-mappers/models/$mcp_audience_id" -r "$APP_KEYCLOAK_REALM" "${mcp_audience[@]}"
else
  kcadm create "clients/$mcp_client_uuid/protocol-mappers/models" -r "$APP_KEYCLOAK_REALM" "${mcp_audience[@]}"
fi
echo "set the audience $mcp_url for MCP tokens"
