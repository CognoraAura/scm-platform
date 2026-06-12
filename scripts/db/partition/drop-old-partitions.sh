#!/bin/bash
# scripts/db/partition/drop-old-partitions.sh
# Drop partitions older than retention period

set -e

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-admin}"
DB_PASSWORD="${DB_PASSWORD:-admin123}"
RETENTION_MONTHS="${RETENTION_MONTHS:-24}"

TABLES=("ord_order" "inv_reservation" "sup_purchase_order")

for TABLE in "${TABLES[@]}"; do
    echo "Dropping old partitions for ${TABLE}..."
    
    CUTOFF_DATE=$(date -d "-${RETENTION_MONTHS} months" +%Y-%m)
    
    PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d db_order -c "
        SELECT partname FROM pg_partitions 
        WHERE tablename = '${TABLE}' 
        AND partname < '${TABLE}_p${CUTOFF_DATE//-/}'
    " -t -A | while read PARTITION; do
        if [ -n "$PARTITION" ]; then
            PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d db_order -c "
                DROP TABLE IF EXISTS ${PARTITION};
            "
            echo "  Dropped ${PARTITION}"
        fi
    done
done

echo "Old partition cleanup complete"