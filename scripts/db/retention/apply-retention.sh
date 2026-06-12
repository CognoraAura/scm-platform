#!/bin/bash
# scripts/db/retention/apply-retention.sh
# Apply data retention policies

set -e

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-admin}"
DB_PASSWORD="${DB_PASSWORD:-admin123}"

echo "Applying data retention policies..."

# Audit logs: keep 2 years
PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d db_audit -c "
    DELETE FROM sys_audit_log WHERE create_time < NOW() - INTERVAL '2 years';
"
echo "  Audit logs: retained 2 years"

# Login history: keep 1 year
PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d db_auth -c "
    DELETE FROM auth_login_history WHERE create_time < NOW() - INTERVAL '1 year';
"
echo "  Login history: retained 1 year"

# API access logs: keep 90 days
PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d db_auth -c "
    DELETE FROM auth_api_access_log WHERE create_time < NOW() - INTERVAL '90 days';
"
echo "  API access logs: retained 90 days"

# Notification history: keep 6 months
PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d db_notify -c "
    DELETE FROM notify_history WHERE create_time < NOW() - INTERVAL '6 months';
"
echo "  Notification history: retained 6 months"

# Order events: keep 3 years (for audit trail)
PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d db_order -c "
    DELETE FROM ord_order_event WHERE create_time < NOW() - INTERVAL '3 years';
"
echo "  Order events: retained 3 years"

echo "Data retention policies applied successfully"
