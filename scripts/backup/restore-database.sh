#!/bin/bash
# scripts/backup/restore-database.sh

set -e

# Configuration
BACKUP_FILE="$1"
DATABASE="$2"
TARGET_HOST="${3:-localhost}"
TARGET_USER="${4:-admin}"

if [ -z "$BACKUP_FILE" ] || [ -z "$DATABASE" ]; then
    echo "Usage: $0 <backup_file> <database> [target_host] [target_user]"
    exit 1
fi

# Extract backup
TEMP_DIR=$(mktemp -d)
tar -xzf "${BACKUP_FILE}" -C "${TEMP_DIR}"

# Restore database
echo "Restoring ${DATABASE} from ${BACKUP_FILE}..."
pg_restore -h "${TARGET_HOST}" -U "${TARGET_USER}" -d "${DATABASE}" -c "${TEMP_DIR}/${DATABASE}.dump"

# Cleanup
rm -rf "${TEMP_DIR}"

echo "Restore completed: ${DATABASE}"
