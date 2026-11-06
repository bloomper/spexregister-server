#!/usr/bin/env bash
# Local development only: fills each secret in .docker/secrets that still holds the placeholder CHANGE_ME with a random
# value, and tells git to leave the files alone. The Keycloak client secrets are left to scripts/configure-keycloak.sh,
# which reads them from the local Keycloak. Values already set are kept, so it can be run again. The database passwords
# take effect only when the databases are created, i.e. with empty data folders.
set -euo pipefail

cd "$(dirname "$0")/.."

random() { openssl rand -base64 96 | tr -dc 'A-Za-z0-9' | head -c "$1"; }

for file in .docker/secrets/*; do
  name=$(basename "$file")
  if [[ "$(cat "$file")" != CHANGE_ME ]]; then
    echo "keep $file"
    continue
  fi
  case $name in
    *_CLIENT_SECRET)
      echo "left $file to scripts/configure-keycloak.sh"
      continue
      ;;
    API_APP_PASSWORD_ENCRYPTION_KEY) value=$(openssl rand -base64 32) ;;
    *) value=$(random 32) ;;
  esac
  printf '%s' "$value" > "$file"
  echo "created $file"
done

git update-index --skip-worktree .docker/secrets/*
