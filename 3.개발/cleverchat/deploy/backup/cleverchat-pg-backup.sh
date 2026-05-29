#!/usr/bin/env sh
set -eu

BACKUP_DIR="${CLEVERCHAT_BACKUP_DIR:-./backups}"
RETENTION_DAYS="${CLEVERCHAT_BACKUP_RETENTION_DAYS:-14}"

case "$RETENTION_DAYS" in
  ''|*[!0-9]*) RETENTION_DAYS=14 ;;
esac

if [ "$RETENTION_DAYS" -lt 7 ]; then
  RETENTION_DAYS=7
fi

for name in PGHOST PGPORT PGDATABASE PGUSER PGPASSWORD; do
  eval "value=\${$name:-}"
  if [ -z "$value" ]; then
    echo "Missing required environment variable: $name" >&2
    exit 2
  fi
done

mkdir -p "$BACKUP_DIR"

timestamp="$(date -u +%Y%m%d-%H%M%S)"
backup_file="$BACKUP_DIR/cleverchat-$PGDATABASE-$timestamp.dump"
checksum_file="$backup_file.sha256"

pg_dump -Fc --no-owner --no-acl --file "$backup_file" "$PGDATABASE"

if command -v sha256sum >/dev/null 2>&1; then
  sha256sum "$backup_file" > "$checksum_file"
else
  shasum -a 256 "$backup_file" > "$checksum_file"
fi

find "$BACKUP_DIR" -type f \( -name "*.dump" -o -name "*.dump.sha256" \) -mtime +"$RETENTION_DAYS" -delete

echo "Backup complete: $backup_file"
echo "Checksum: $checksum_file"
