#!/usr/bin/env sh
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
VERSION_ARGS=""
if [ -n "${VERSION_BUMP_OVERRIDE:-}" ]; then
  VERSION_ARGS="--override=${VERSION_BUMP_OVERRIDE}"
fi
# shellcheck disable=SC2086
"$PROJECT_DIR/version.sh" $VERSION_ARGS
ENV_FILE="$PROJECT_DIR/target/generated-version/build-version.env"
VERSION_RELEASE=$(sed -n 's/^VERSION_RELEASE=//p' "$ENV_FILE" | head -n 1)
VERSION_TIMESTAMP=$(sed -n 's/^VERSION_TIMESTAMP=//p' "$ENV_FILE" | head -n 1)
if [ -z "$VERSION_RELEASE" ]; then
  echo "HIBA: A generalt verzioszam nem olvashato." >&2
  exit 1
fi
DEV_REVISION="${VERSION_RELEASE}-SNAPSHOT"

echo
echo "[1/2] Saját modulok telepitese a lokalis Maven repositoryba: $DEV_REVISION"
cd "$PROJECT_DIR"
mvn -U -pl :nav-xsd-parser-tool-web -am install -DskipTests \
  "-Drevision=$DEV_REVISION" \
  "-Dapp.release.version=$VERSION_RELEASE" \
  "-Dapp.build.timestamp=$VERSION_TIMESTAMP"

echo
echo "[2/2] Web alkalmazas inditasa Spring Boot-tal..."
exec mvn -f "$PROJECT_DIR/nav-xsd-parser-tool-web/pom.xml" spring-boot:run \
  "-Drevision=$DEV_REVISION" \
  "-Dapp.release.version=$VERSION_RELEASE" \
  "-Dapp.build.timestamp=$VERSION_TIMESTAMP" "$@"
