#!/bin/bash
# scripts/db/quality/check-quality.sh
# Run data quality checks

set -e

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-admin}"
DB_PASSWORD="${DB_PASSWORD:-admin123}"

echo "Running data quality checks..."

# Check for orphaned order items
PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d db_order -c "
    SELECT COUNT(*) as orphaned_items 
    FROM ord_order_item oi 
    LEFT JOIN ord_order o ON oi.order_id = o.id 
    WHERE o.id IS NULL;
" -t -A

# Check for negative inventory
PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d db_inventory -c "
    SELECT COUNT(*) as negative_stock 
    FROM inv_stock 
    WHERE available_qty < 0;
" -t -A

# Check for duplicate SKUs
PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d db_product -c "
    SELECT sku_code, COUNT(*) as duplicate_count 
    FROM pro_sku 
    GROUP BY sku_code 
    HAVING COUNT(*) > 1;
" -t -A

echo "Data quality checks complete"
