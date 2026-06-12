-- scripts/db/migration/V001__baseline.sql
-- This script documents the current schema state
-- Run: psql -h localhost -U admin -d db_order -f scripts/db/migration/V001__baseline.sql

-- Mark existing schema as baseline
-- Flyway will not execute this file, but will record it as applied
SELECT 1;
