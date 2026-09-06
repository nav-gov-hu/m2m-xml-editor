#!/bin/sh
set -eu

BOOTSTRAP_FILE=""
JDBC_URL=""
USERNAME=""
ALLOW_EMPTY=0

while [ "$#" -gt 0 ]; do
  case "$1" in
    --bootstrap-file) BOOTSTRAP_FILE=${2:?}; shift 2 ;;
    --jdbc-url) JDBC_URL=${2:?}; shift 2 ;;
    --username) USERNAME=${2:?}; shift 2 ;;
    --allow-empty-password) ALLOW_EMPTY=1; shift ;;
    *) echo "Ismeretlen paraméter: $1" >&2; exit 2 ;;
  esac
done

resolve_bootstrap_file() {
  if [ -n "$BOOTSTRAP_FILE" ]; then
    printf '%s\n' "$BOOTSTRAP_FILE"
    return
  fi
  locator="$HOME/.m2m-xml-editor/bootstrap-location.properties"
  if [ -f "$locator" ]; then
    candidate=$(sed -n 's/^[[:space:]]*bootstrap\.file[[:space:]]*=[[:space:]]*//p' "$locator" | head -n 1)
    if [ -n "$candidate" ]; then
      printf '%s\n' "$candidate"
      return
    fi
  fi
  if [ -n "${M2M_XML_EDITOR_HOME:-}" ]; then
    printf '%s/config/application-bootstrap.properties\n' "$M2M_XML_EDITOR_HOME"
    return
  fi
  printf '%s/.local/share/m2m-xml-editor/config/application-bootstrap.properties\n' "$HOME"
}

escape_value() {
  # Java Properties readerrel kompatibilis alap escape-ek.
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/	/\\t/g'
}

set_property() {
  key=$1
  value=$2
  escaped=$(escape_value "$value")
  awk -v k="$key" -v v="$escaped" '
    BEGIN { done=0 }
    $0 ~ "^[[:space:]]*" k "[[:space:]]*[:=]" {
      if (!done) print k "=" v
      done=1
      next
    }
    { print }
    END { if (!done) print k "=" v }
  ' "$TARGET" > "$TEMP"
  mv "$TEMP" "$TARGET"
}

TARGET=$(resolve_bootstrap_file)
if [ ! -f "$TARGET" ]; then
  echo "A bootstrap konfiguráció nem található: $TARGET" >&2
  exit 1
fi

printf 'Bootstrap konfiguráció: %s\n' "$TARGET"
echo 'Ez az eszköz csak a kapcsolódási konfigurációt módosítja; az adatbázis-felhasználó jelszavát NEM változtatja meg.'

if [ -z "$JDBC_URL" ]; then
  printf 'Új JDBC URL (Enter = maradjon változatlan): '
  IFS= read -r JDBC_URL || true
fi
if [ -z "$USERNAME" ]; then
  printf 'Új adatbázis-felhasználó (Enter = maradjon változatlan): '
  IFS= read -r USERNAME || true
fi

printf 'Új adatbázis-jelszó: '
if [ -t 0 ]; then
  stty -echo
  IFS= read -r PASSWORD || true
  stty echo
else
  IFS= read -r PASSWORD || true
fi
printf '\n'
TEMP="$TARGET.tmp.$$"
trap 'if [ -t 0 ]; then stty echo 2>/dev/null || true; fi; rm -f "$TEMP" 2>/dev/null || true' EXIT HUP INT TERM

if [ -z "$PASSWORD" ] && [ "$ALLOW_EMPTY" -ne 1 ]; then
  printf 'Üres adatbázis-jelszót adtál meg. Biztosan ezt szeretnéd? Írd be: IGEN: '
  IFS= read -r confirmation || true
  if [ "$confirmation" != "IGEN" ]; then
    echo 'A datasource konfiguráció módosítása megszakítva.' >&2
    exit 1
  fi
fi

cp -f "$TARGET" "$TARGET.bak"
TEMP="$TARGET.tmp.$$"
: > "$TEMP"
if [ -n "$JDBC_URL" ]; then set_property 'spring.datasource.url' "$JDBC_URL"; fi
TEMP="$TARGET.tmp.$$"
: > "$TEMP"
if [ -n "$USERNAME" ]; then set_property 'spring.datasource.username' "$USERNAME"; fi
TEMP="$TARGET.tmp.$$"
: > "$TEMP"
set_property 'spring.datasource.password' "$PASSWORD"

PASSWORD=''
trap - EXIT HUP INT TERM
echo 'A datasource bootstrap konfiguráció frissítve.'
printf 'Biztonsági mentés: %s.bak\n' "$TARGET"
echo 'Indítsd újra az alkalmazást. Ha a kapcsolat továbbra sem működik, az adatbázis oldali hitelesítést az üzemeltetővel kell ellenőrizni.'
