#!/usr/bin/env sh
set -eu

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
SOURCE_FILE="$PROJECT_DIR/nav-xsd-parser-tool-versioning/src/main/java/hu/gov/nav/xsdparsertool/versioning/VersioningTool.java"
VERSION_BUILD_DIR="$PROJECT_DIR/target/versioning-bootstrap"
VERSION_CLASS="hu.gov.nav.xsdparsertool.versioning.VersioningTool"

command -v java >/dev/null 2>&1 || { echo "HIBA: A java nincs a PATH-ban. JDK 17 vagy ujabb szukseges." >&2; exit 1; }
command -v javac >/dev/null 2>&1 || { echo "HIBA: A javac nincs a PATH-ban. Teljes JDK 17 vagy ujabb szukseges." >&2; exit 1; }
[ -f "$SOURCE_FILE" ] || { echo "HIBA: A verzioelemzo forrasa nem talalhato: $SOURCE_FILE" >&2; exit 1; }

rm -rf "$VERSION_BUILD_DIR"
mkdir -p "$VERSION_BUILD_DIR"
javac -encoding UTF-8 -d "$VERSION_BUILD_DIR" "$SOURCE_FILE"

cd "$PROJECT_DIR"
java -cp "$VERSION_BUILD_DIR" "$VERSION_CLASS" --repo="$PROJECT_DIR" "$@"

[ -f "$PROJECT_DIR/target/generated-version/build-version.env" ] || {
  echo "HIBA: A verzioelemzo nem hozta letre a build-version.env fajlt." >&2
  exit 1
}
