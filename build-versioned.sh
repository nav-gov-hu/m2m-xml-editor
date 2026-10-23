#!/usr/bin/env sh
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
RELEASE_FILE="$PROJECT_DIR/RELEASE"

if [ -e "$PROJECT_DIR/.git" ]; then
  command -v git >/dev/null 2>&1 || { echo 'HIBA: Git metadata van, de a git parancs nem erheto el.' >&2; exit 1; }
  if [ -n "${VERSION_BUMP_OVERRIDE:-}" ]; then
    "$PROJECT_DIR/version.sh" "--override=$VERSION_BUMP_OVERRIDE"
  else
    "$PROJECT_DIR/version.sh"
  fi
  # shellcheck disable=SC1091
  . "$PROJECT_DIR/target/generated-version/build-version.env"
  FULL_RELEASE="$VERSION_RELEASE-$VERSION_TIMESTAMP"
  printf '%s\n' "$FULL_RELEASE" > "$RELEASE_FILE"
else
  [ -z "${VERSION_BUMP_OVERRIDE:-}" ] || { echo 'HIBA: VERSION_BUMP_OVERRIDE csak Git-repozitoriumban hasznalhato.' >&2; exit 1; }
  [ -f "$RELEASE_FILE" ] || { echo 'HIBA: Git nelkuli buildhez hianyzik a RELEASE fajl.' >&2; exit 1; }
  IFS= read -r FULL_RELEASE < "$RELEASE_FILE" || [ -n "$FULL_RELEASE" ]
  if ! printf '%s\n' "$FULL_RELEASE" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+-[0-9]{8}-[0-9]{6}(-RC[0-9]+)?$'; then
    echo "HIBA: Ervenytelen RELEASE formatum: $FULL_RELEASE" >&2
    exit 1
  fi
  VERSION_RELEASE=${FULL_RELEASE%%-*}
  VERSION_TIMESTAMP=${FULL_RELEASE#*-}
  VERSION_TIMESTAMP=$(printf '%s\n' "$VERSION_TIMESTAMP" | cut -d - -f 1-2)
fi

echo "Maven release version: $VERSION_RELEASE"
echo "Full release: $FULL_RELEASE"
echo "Build timestamp: $VERSION_TIMESTAMP"
cd "$PROJECT_DIR"
mvn clean package "-Drevision=$VERSION_RELEASE" "-Dapp.release.version=$VERSION_RELEASE" "-Dapp.build.timestamp=$VERSION_TIMESTAMP" "-Dapp.full.release=$FULL_RELEASE" "$@"
