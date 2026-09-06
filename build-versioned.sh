#!/usr/bin/env sh
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if [ -n "${VERSION_BUMP_OVERRIDE:-}" ]; then
  "$PROJECT_DIR/version.sh" "--override=$VERSION_BUMP_OVERRIDE"
else
  "$PROJECT_DIR/version.sh"
fi
# shellcheck disable=SC1091
. "$PROJECT_DIR/target/generated-version/build-version.env"
echo "Maven release version: $VERSION_RELEASE"
echo "Build timestamp: $VERSION_TIMESTAMP"
cd "$PROJECT_DIR"
mvn clean package "-Drevision=$VERSION_RELEASE" "-Dapp.release.version=$VERSION_RELEASE" "-Dapp.build.timestamp=$VERSION_TIMESTAMP" "$@"
