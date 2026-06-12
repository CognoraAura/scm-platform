#!/bin/bash
# scripts/db/partition/create-partitions.sh
# Auto-create monthly partitions for partitioned tables

set -e

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-admin}"
DB_PASSWORD="${DB_PASSWORD:-admin123}"

TABLES=("ord_order" "inv_reservation" "sup_purchase_order")

for TABLE in "${TABLES[@]}"; do
    echo "Creating partitions for ${TABLE}..."
    
    # Create partitions for next 3 months
    for i in $(seq 0 2); do
        PARTITION_DATE=$(date -d "+${i} months" +%Y-%m)
        PARTITION_NAME="${TABLE}_p${PARTITION_DATE//-/}"
        
        PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d db_order -c "
            CREATE TABLE IF NOT EXISTS ${PARTITION_NAME} 
            PARTITION OF ${TABLE} 
            FOR VALUES FROM ('${PARTITION_DATE}-01') TO ('$(date -d "${PARTITION_DATE}-01 +1 month" +%Y-%m)-01');
        " 2>/dev/null || true
        
        echo "  Partition ${PARTITION_NAME} ensured"
    done
done

echo "Partition management complete"