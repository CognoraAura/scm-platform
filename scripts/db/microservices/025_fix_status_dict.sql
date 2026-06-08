-- ============================================================
-- 修复状态字典：对齐 Java 枚举
-- 1. INBOUND: FINISHED → COMPLETED，新增 PARTIAL
-- 2. 新增 WAVE_PICKING 状态字典
-- ============================================================

\c db_permission

-- ============================================================
-- 1. 修复入库状态 (INBOUND)
-- ============================================================

-- 1a. 将 FINISHED 改名为 COMPLETED
UPDATE sys_status_dict
SET status_code = 'COMPLETED',
    status_name = '已完成',
    status_name_en = 'Completed'
WHERE biz_type = 'INBOUND' AND status_code = 'FINISHED';

-- 1b. 新增 PARTIAL (部分入库) 状态
INSERT INTO sys_status_dict (id, tenant_id, biz_type, status_code, status_name, status_name_en, color, icon, sort_order, is_initial, is_terminal, is_cancellable, enabled)
VALUES ('sd-inbound-04', NULL, 'INBOUND', 'PARTIAL', '部分入库', 'Partial', '#faad14', 'warning', 2, false, false, true, true)
ON CONFLICT DO NOTHING;

-- 1c. 更新 COMPLETED 的 sort_order 为 3
UPDATE sys_status_dict
SET sort_order = 3
WHERE biz_type = 'INBOUND' AND status_code = 'COMPLETED';

-- 1d. 更新 CANCELLED 的 sort_order 为 4
UPDATE sys_status_dict
SET sort_order = 4
WHERE biz_type = 'INBOUND' AND status_code = 'CANCELLED';

-- 1e. 更新入库流转规则
-- 删除旧的 PROCESSING → FINISHED 规则
DELETE FROM sys_status_transition
WHERE biz_type = 'INBOUND' AND from_status = 'PROCESSING' AND to_status = 'FINISHED';

-- 新增 PROCESSING → PARTIAL (部分入库)
INSERT INTO sys_status_transition (id, tenant_id, biz_type, from_status, to_status, action_code, action_name, action_name_en, need_approval, enabled, sort_order)
VALUES ('st-inbound-04', NULL, 'INBOUND', 'PROCESSING', 'PARTIAL', 'PARTIAL_COMPLETE', '部分完成', 'Partial Complete', false, true, 1)
ON CONFLICT DO NOTHING;

-- 新增 PROCESSING → COMPLETED (全部完成)
INSERT INTO sys_status_transition (id, tenant_id, biz_type, from_status, to_status, action_code, action_name, action_name_en, need_approval, enabled, sort_order)
VALUES ('st-inbound-05', NULL, 'INBOUND', 'PROCESSING', 'COMPLETED', 'COMPLETE', '完成', 'Complete', false, true, 0)
ON CONFLICT DO NOTHING;

-- 新增 PARTIAL → COMPLETED (继续完成)
INSERT INTO sys_status_transition (id, tenant_id, biz_type, from_status, to_status, action_code, action_name, action_name_en, need_approval, enabled, sort_order)
VALUES ('st-inbound-06', NULL, 'INBOUND', 'PARTIAL', 'COMPLETED', 'COMPLETE', '完成', 'Complete', false, true, 0)
ON CONFLICT DO NOTHING;

-- 新增 PARTIAL → CANCELLED (部分入库后取消)
INSERT INTO sys_status_transition (id, tenant_id, biz_type, from_status, to_status, action_code, action_name, action_name_en, need_approval, enabled, sort_order)
VALUES ('st-inbound-07', NULL, 'INBOUND', 'PARTIAL', 'CANCELLED', 'CANCEL', '取消', 'Cancel', true, true, 1)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 2. 新增波次拣货状态 (WAVE_PICKING)
-- ============================================================

INSERT INTO sys_status_dict (id, tenant_id, biz_type, status_code, status_name, status_name_en, color, icon, sort_order, is_initial, is_terminal, is_cancellable, enabled) VALUES
('sd-wave-00', NULL, 'WAVE_PICKING', 'WAITING', '待拣货', 'Waiting', '#faad14', 'clock-circle', 0, true, false, true, true),
('sd-wave-01', NULL, 'WAVE_PICKING', 'PICKING', '拣货中', 'Picking', '#1890ff', 'shopping-cart', 1, false, false, false, true),
('sd-wave-02', NULL, 'WAVE_PICKING', 'COMPLETED', '已完成', 'Completed', '#52c41a', 'check-circle', 2, false, true, false, true),
('sd-wave-03', NULL, 'WAVE_PICKING', 'CANCELLED', '已取消', 'Cancelled', '#ff4d4f', 'close-circle', 3, false, true, false, true);

INSERT INTO sys_status_transition (id, tenant_id, biz_type, from_status, to_status, action_code, action_name, action_name_en, need_approval, enabled, sort_order) VALUES
('st-wave-01', NULL, 'WAVE_PICKING', 'WAITING', 'PICKING', 'START_PICK', '开始拣货', 'Start Pick', false, true, 0),
('st-wave-02', NULL, 'WAVE_PICKING', 'WAITING', 'CANCELLED', 'CANCEL', '取消', 'Cancel', false, true, 1),
('st-wave-03', NULL, 'WAVE_PICKING', 'PICKING', 'COMPLETED', 'COMPLETE', '完成', 'Complete', false, true, 0),
('st-wave-04', NULL, 'WAVE_PICKING', 'PICKING', 'CANCELLED', 'CANCEL', '取消', 'Cancel', true, true, 1);
