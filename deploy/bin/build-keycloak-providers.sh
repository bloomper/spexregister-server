#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
deploy_dir=$PWD
server_dir=$(cd .. && pwd)
theme_dir=$(cd "${THEME_SOURCE_DIR:-../../fgv-keycloakify}" && pwd)
owner="$(id -u):$(id -g)"

docker run --rm -u "$owner" -v "$server_dir:/src" -w /src \
  -e GRADLE_USER_HOME=/src/build/gradle-home \
  eclipse-temurin:25-jdk ./gradlew --no-daemon -q keycloakJar

docker run --rm -v "$theme_dir:/src" -w /src node:22-bookworm sh -c "
  apt-get update -qq &&
  apt-get install -y -qq --no-install-recommends maven openjdk-17-jdk-headless >/dev/null &&
  npm ci --no-audit --no-fund --loglevel=error &&
  npm run build-keycloak-theme;
  status=\$?; chown -R $owner /src; exit \$status"

mkdir -p keycloak/providers
rm -f keycloak/providers/*.jar
cp "$server_dir"/build/keycloak-providers/*-keycloak.jar keycloak/providers/
cp "$theme_dir"/dist_keycloak/keycloak-theme-for-kc-all-other-versions.jar keycloak/providers/
ls -l "$deploy_dir"/keycloak/providers/*.jar
