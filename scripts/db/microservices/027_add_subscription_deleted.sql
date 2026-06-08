-- ============================================================
-- 为 tenant_subscription 表添加 deleted 字段
-- ============================================================

\c db_tenant

ALTER TABLE tenant_subscription ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
COMMENT ON COLUMN tenant_subscription.deleted IS '软删除标记';
