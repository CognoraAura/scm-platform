-- ============================================================
-- 乐观锁 version 字段迁移
-- 为关键业务表添加 version 列，支持状态流转 CAS 更新
-- ============================================================

-- 订单表
ALTER TABLE ord_order ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
COMMENT ON COLUMN ord_order.version IS '乐观锁版本号';

-- 入库单表
ALTER TABLE wms_inbound ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
COMMENT ON COLUMN wms_inbound.version IS '乐观锁版本号';

-- 出库单表
ALTER TABLE wms_outbound ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
COMMENT ON COLUMN wms_outbound.version IS '乐观锁版本号';

-- 采购单表
ALTER TABLE sup_purchase_order ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
COMMENT ON COLUMN sup_purchase_order.version IS '乐观锁版本号';

-- 物流运单表
ALTER TABLE tms_waybill ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
COMMENT ON COLUMN tms_waybill.version IS '乐观锁版本号';
