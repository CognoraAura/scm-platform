-- ============================================================
-- 状态字典初始数据
-- 为 ORDER, PURCHASE, INBOUND, OUTBOUND, APPROVAL 预置默认状态
-- ============================================================

\c db_permission

-- ============================================================
-- 订单状态 (ORDER)
-- ============================================================
INSERT INTO sys_status_dict (id, tenant_id, biz_type, status_code, status_name, status_name_en, color, icon, sort_order, is_initial, is_terminal, is_cancellable, enabled) VALUES
('sd-order-00', NULL, 'ORDER', 'PENDING_PAYMENT', '待支付', 'Pending Payment', '#faad14', 'clock-circle', 0, true, false, true, true),
('sd-order-01', NULL, 'ORDER', 'PAID', '已支付', 'Paid', '#52c41a', 'check-circle', 1, false, false, true, true),
('sd-order-02', NULL, 'ORDER', 'PENDING_SHIP', '待发货', 'Pending Shipment', '#1890ff', 'inbox', 2, false, false, false, true),
('sd-order-03', NULL, 'ORDER', 'SHIPPED', '已发货', 'Shipped', '#1890ff', 'car', 3, false, false, false, true),
('sd-order-04', NULL, 'ORDER', 'IN_TRANSIT', '运输中', 'In Transit', '#722ed1', 'rocket', 4, false, false, false, true),
('sd-order-05', NULL, 'ORDER', 'DELIVERED', '已送达', 'Delivered', '#52c41a', 'check-circle', 5, false, false, false, true),
('sd-order-06', NULL, 'ORDER', 'COMPLETED', '已完成', 'Completed', '#52c41a', 'smile', 6, false, true, false, true),
('sd-order-07', NULL, 'ORDER', 'CANCELLED', '已取消', 'Cancelled', '#ff4d4f', 'close-circle', 7, false, true, false, true),
('sd-order-08', NULL, 'ORDER', 'REFUNDING', '退款中', 'Refunding', '#faad14', 'loading', 8, false, false, false, true),
('sd-order-09', NULL, 'ORDER', 'REFUNDED', '已退款', 'Refunded', '#ff4d4f', 'undo', 9, false, true, false, true);

-- 订单流转规则
INSERT INTO sys_status_transition (id, tenant_id, biz_type, from_status, to_status, action_code, action_name, action_name_en, need_approval, enabled, sort_order) VALUES
('st-order-01', NULL, 'ORDER', 'PENDING_PAYMENT', 'PAID', 'PAY', '支付', 'Pay', false, true, 0),
('st-order-02', NULL, 'ORDER', 'PENDING_PAYMENT', 'CANCELLED', 'CANCEL', '取消', 'Cancel', false, true, 1),
('st-order-03', NULL, 'ORDER', 'PAID', 'PENDING_SHIP', 'ALLOCATE', '分配', 'Allocate', false, true, 0),
('st-order-04', NULL, 'ORDER', 'PAID', 'CANCELLED', 'CANCEL', '取消', 'Cancel', true, true, 1),
('st-order-05', NULL, 'ORDER', 'PAID', 'REFUNDING', 'REFUND', '退款', 'Refund', true, true, 2),
('st-order-06', NULL, 'ORDER', 'PENDING_SHIP', 'SHIPPED', 'SHIP', '发货', 'Ship', false, true, 0),
('st-order-07', NULL, 'ORDER', 'PENDING_SHIP', 'REFUNDING', 'REFUND', '退款', 'Refund', true, true, 1),
('st-order-08', NULL, 'ORDER', 'SHIPPED', 'IN_TRANSIT', 'DISPATCH', '派送', 'Dispatch', false, true, 0),
('st-order-09', NULL, 'ORDER', 'SHIPPED', 'REFUNDING', 'REFUND', '退款', 'Refund', true, true, 1),
('st-order-10', NULL, 'ORDER', 'IN_TRANSIT', 'DELIVERED', 'DELIVER', '签收', 'Deliver', false, true, 0),
('st-order-11', NULL, 'ORDER', 'IN_TRANSIT', 'REFUNDING', 'REFUND', '退款', 'Refund', true, true, 1),
('st-order-12', NULL, 'ORDER', 'DELIVERED', 'COMPLETED', 'COMPLETE', '完成', 'Complete', false, true, 0),
('st-order-13', NULL, 'ORDER', 'DELIVERED', 'REFUNDING', 'REFUND', '退款', 'Refund', true, true, 1),
('st-order-14', NULL, 'ORDER', 'REFUNDING', 'REFUNDED', 'CONFIRM_REFUND', '确认退款', 'Confirm Refund', false, true, 0);

-- ============================================================
-- 采购状态 (PURCHASE)
-- ============================================================
INSERT INTO sys_status_dict (id, tenant_id, biz_type, status_code, status_name, status_name_en, color, icon, sort_order, is_initial, is_terminal, is_cancellable, enabled) VALUES
('sd-purchase-00', NULL, 'PURCHASE', 'DRAFT', '草稿', 'Draft', '#d9d9d9', 'file', 0, true, false, true, true),
('sd-purchase-01', NULL, 'PURCHASE', 'PENDING_APPROVAL', '待审批', 'Pending Approval', '#faad14', 'clock-circle', 1, false, false, true, true),
('sd-purchase-02', NULL, 'PURCHASE', 'APPROVED', '已审批', 'Approved', '#52c41a', 'check-circle', 2, false, false, false, true),
('sd-purchase-03', NULL, 'PURCHASE', 'REJECTED', '已驳回', 'Rejected', '#ff4d4f', 'close-circle', 3, false, true, false, true),
('sd-purchase-04', NULL, 'PURCHASE', 'ORDERED', '已下单', 'Ordered', '#1890ff', 'shopping-cart', 4, false, false, false, true),
('sd-purchase-05', NULL, 'PURCHASE', 'RECEIVING', '收货中', 'Receiving', '#722ed1', 'inbox', 5, false, false, false, true),
('sd-purchase-06', NULL, 'PURCHASE', 'COMPLETED', '已完成', 'Completed', '#52c41a', 'smile', 6, false, true, false, true),
('sd-purchase-07', NULL, 'PURCHASE', 'CANCELLED', '已取消', 'Cancelled', '#ff4d4f', 'close-circle', 7, false, true, false, true);

-- 采购流转规则
INSERT INTO sys_status_transition (id, tenant_id, biz_type, from_status, to_status, action_code, action_name, action_name_en, need_approval, enabled, sort_order) VALUES
('st-purchase-01', NULL, 'PURCHASE', 'DRAFT', 'PENDING_APPROVAL', 'SUBMIT', '提交', 'Submit', false, true, 0),
('st-purchase-02', NULL, 'PURCHASE', 'DRAFT', 'CANCELLED', 'CANCEL', '取消', 'Cancel', false, true, 1),
('st-purchase-03', NULL, 'PURCHASE', 'PENDING_APPROVAL', 'APPROVED', 'APPROVE', '审批通过', 'Approve', false, true, 0),
('st-purchase-04', NULL, 'PURCHASE', 'PENDING_APPROVAL', 'REJECTED', 'REJECT', '驳回', 'Reject', false, true, 1),
('st-purchase-05', NULL, 'PURCHASE', 'APPROVED', 'ORDERED', 'PLACE_ORDER', '下单', 'Place Order', false, true, 0),
('st-purchase-06', NULL, 'PURCHASE', 'APPROVED', 'CANCELLED', 'CANCEL', '取消', 'Cancel', true, true, 1),
('st-purchase-07', NULL, 'PURCHASE', 'ORDERED', 'RECEIVING', 'START_RECEIVE', '开始收货', 'Start Receive', false, true, 0),
('st-purchase-08', NULL, 'PURCHASE', 'RECEIVING', 'COMPLETED', 'COMPLETE', '完成', 'Complete', false, true, 0);

-- ============================================================
-- 采购申请状态 (PURCHASE_REQUEST)
-- ============================================================
INSERT INTO sys_status_dict (id, tenant_id, biz_type, status_code, status_name, status_name_en, color, icon, sort_order, is_initial, is_terminal, is_cancellable, enabled) VALUES
('sd-preq-00', NULL, 'PURCHASE_REQUEST', 'DRAFT', '草稿', 'Draft', '#d9d9d9', 'file', 0, true, false, true, true),
('sd-preq-01', NULL, 'PURCHASE_REQUEST', 'PENDING_APPROVAL', '待审批', 'Pending Approval', '#faad14', 'clock-circle', 1, false, false, true, true),
('sd-preq-02', NULL, 'PURCHASE_REQUEST', 'APPROVED', '已审批', 'Approved', '#52c41a', 'check-circle', 2, false, false, false, true),
('sd-preq-03', NULL, 'PURCHASE_REQUEST', 'REJECTED', '已驳回', 'Rejected', '#ff4d4f', 'close-circle', 3, false, true, false, true),
('sd-preq-04', NULL, 'PURCHASE_REQUEST', 'CONVERTED', '已转采购单', 'Converted', '#1890ff', 'shopping-cart', 4, false, true, false, true),
('sd-preq-05', NULL, 'PURCHASE_REQUEST', 'CLOSED', '已关闭', 'Closed', '#d9d9d9', 'lock', 5, false, true, false, true);

INSERT INTO sys_status_transition (id, tenant_id, biz_type, from_status, to_status, action_code, action_name, action_name_en, need_approval, enabled, sort_order) VALUES
('st-preq-01', NULL, 'PURCHASE_REQUEST', 'DRAFT', 'PENDING_APPROVAL', 'SUBMIT', '提交', 'Submit', false, true, 0),
('st-preq-02', NULL, 'PURCHASE_REQUEST', 'DRAFT', 'CLOSED', 'CLOSE', '关闭', 'Close', false, true, 1),
('st-preq-03', NULL, 'PURCHASE_REQUEST', 'PENDING_APPROVAL', 'APPROVED', 'APPROVE', '审批通过', 'Approve', false, true, 0),
('st-preq-04', NULL, 'PURCHASE_REQUEST', 'PENDING_APPROVAL', 'REJECTED', 'REJECT', '驳回', 'Reject', false, true, 1),
('st-preq-05', NULL, 'PURCHASE_REQUEST', 'APPROVED', 'CONVERTED', 'CONVERT', '转采购单', 'Convert to Order', false, true, 0),
('st-preq-06', NULL, 'PURCHASE_REQUEST', 'APPROVED', 'CLOSED', 'CLOSE', '关闭', 'Close', false, true, 1);

-- ============================================================
-- 入库状态 (INBOUND)
INSERT INTO sys_status_dict (id, tenant_id, biz_type, status_code, status_name, status_name_en, color, icon, sort_order, is_initial, is_terminal, is_cancellable, enabled) VALUES
('sd-inbound-00', NULL, 'INBOUND', 'WAITING', '待入库', 'Waiting', '#faad14', 'clock-circle', 0, true, false, true, true),
('sd-inbound-01', NULL, 'INBOUND', 'PROCESSING', '入库中', 'Processing', '#1890ff', 'loading', 1, false, false, false, true),
('sd-inbound-04', NULL, 'INBOUND', 'PARTIAL', '部分入库', 'Partial', '#faad14', 'warning', 2, false, false, true, true),
('sd-inbound-02', NULL, 'INBOUND', 'COMPLETED', '已完成', 'Completed', '#52c41a', 'check-circle', 3, false, true, false, true),
('sd-inbound-03', NULL, 'INBOUND', 'CANCELLED', '已取消', 'Cancelled', '#ff4d4f', 'close-circle', 4, false, true, false, true);

INSERT INTO sys_status_transition (id, tenant_id, biz_type, from_status, to_status, action_code, action_name, action_name_en, need_approval, enabled, sort_order) VALUES
('st-inbound-01', NULL, 'INBOUND', 'WAITING', 'PROCESSING', 'START', '开始入库', 'Start', false, true, 0),
('st-inbound-02', NULL, 'INBOUND', 'WAITING', 'CANCELLED', 'CANCEL', '取消', 'Cancel', false, true, 1),
('st-inbound-05', NULL, 'INBOUND', 'PROCESSING', 'COMPLETED', 'COMPLETE', '完成', 'Complete', false, true, 0),
('st-inbound-04', NULL, 'INBOUND', 'PROCESSING', 'PARTIAL', 'PARTIAL_COMPLETE', '部分完成', 'Partial Complete', false, true, 1),
('st-inbound-06', NULL, 'INBOUND', 'PARTIAL', 'COMPLETED', 'COMPLETE', '完成', 'Complete', false, true, 0),
('st-inbound-07', NULL, 'INBOUND', 'PARTIAL', 'CANCELLED', 'CANCEL', '取消', 'Cancel', true, true, 1);

-- 波次拣货状态 (WAVE_PICKING)
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

-- ============================================================
-- 出库状态 (OUTBOUND)
-- ============================================================
INSERT INTO sys_status_dict (id, tenant_id, biz_type, status_code, status_name, status_name_en, color, icon, sort_order, is_initial, is_terminal, is_cancellable, enabled) VALUES
('sd-outbound-00', NULL, 'OUTBOUND', 'WAITING', '待出库', 'Waiting', '#faad14', 'clock-circle', 0, true, false, true, true),
('sd-outbound-01', NULL, 'OUTBOUND', 'PICKING', '拣货中', 'Picking', '#1890ff', 'shopping-cart', 1, false, false, false, true),
('sd-outbound-02', NULL, 'OUTBOUND', 'PACKED', '已打包', 'Packed', '#722ed1', 'gift', 2, false, false, false, true),
('sd-outbound-03', NULL, 'OUTBOUND', 'SHIPPED', '已发货', 'Shipped', '#52c41a', 'car', 3, false, true, false, true),
('sd-outbound-04', NULL, 'OUTBOUND', 'CANCELLED', '已取消', 'Cancelled', '#ff4d4f', 'close-circle', 4, false, true, false, true);

INSERT INTO sys_status_transition (id, tenant_id, biz_type, from_status, to_status, action_code, action_name, action_name_en, need_approval, enabled, sort_order) VALUES
('st-outbound-01', NULL, 'OUTBOUND', 'WAITING', 'PICKING', 'START_PICK', '开始拣货', 'Start Pick', false, true, 0),
('st-outbound-02', NULL, 'OUTBOUND', 'WAITING', 'CANCELLED', 'CANCEL', '取消', 'Cancel', false, true, 1),
('st-outbound-03', NULL, 'OUTBOUND', 'PICKING', 'PACKED', 'PACK', '打包', 'Pack', false, true, 0),
('st-outbound-04', NULL, 'OUTBOUND', 'PACKED', 'SHIPPED', 'SHIP', '发货', 'Ship', false, true, 0);

-- ============================================================
-- 审批状态 (APPROVAL)
-- ============================================================
INSERT INTO sys_status_dict (id, tenant_id, biz_type, status_code, status_name, status_name_en, color, icon, sort_order, is_initial, is_terminal, is_cancellable, enabled) VALUES
('sd-approval-00', NULL, 'APPROVAL', 'PENDING', '待审批', 'Pending', '#faad14', 'clock-circle', 0, true, false, true, true),
('sd-approval-01', NULL, 'APPROVAL', 'IN_PROGRESS', '审批中', 'In Progress', '#1890ff', 'loading', 1, false, false, false, true),
('sd-approval-02', NULL, 'APPROVAL', 'APPROVED', '已通过', 'Approved', '#52c41a', 'check-circle', 2, false, true, false, true),
('sd-approval-03', NULL, 'APPROVAL', 'REJECTED', '已驳回', 'Rejected', '#ff4d4f', 'close-circle', 3, false, true, false, true),
('sd-approval-04', NULL, 'APPROVAL', 'CANCELLED', '已撤回', 'Cancelled', '#d9d9d9', 'undo', 4, false, true, false, true);

INSERT INTO sys_status_transition (id, tenant_id, biz_type, from_status, to_status, action_code, action_name, action_name_en, need_approval, enabled, sort_order) VALUES
('st-approval-01', NULL, 'APPROVAL', 'PENDING', 'IN_PROGRESS', 'START', '开始审批', 'Start', false, true, 0),
('st-approval-02', NULL, 'APPROVAL', 'PENDING', 'CANCELLED', 'CANCEL', '撤回', 'Cancel', false, true, 1),
('st-approval-03', NULL, 'APPROVAL', 'IN_PROGRESS', 'APPROVED', 'APPROVE', '通过', 'Approve', false, true, 0),
('st-approval-04', NULL, 'APPROVAL', 'IN_PROGRESS', 'REJECTED', 'REJECT', '驳回', 'Reject', false, true, 1);

-- ============================================================
-- 物流状态 (LOGISTICS)
-- ============================================================
INSERT INTO sys_status_dict (id, tenant_id, biz_type, status_code, status_name, status_name_en, color, icon, sort_order, is_initial, is_terminal, is_cancellable, enabled) VALUES
('sd-logistics-00', NULL, 'LOGISTICS', 'CREATED', '已创建', 'Created', '#d9d9d9', 'file', 0, true, false, true, true),
('sd-logistics-01', NULL, 'LOGISTICS', 'PENDING', '待发货', 'Pending', '#faad14', 'clock-circle', 1, false, false, true, true),
('sd-logistics-02', NULL, 'LOGISTICS', 'IN_TRANSIT', '运输中', 'In Transit', '#1890ff', 'rocket', 2, false, false, false, true),
('sd-logistics-03', NULL, 'LOGISTICS', 'DELIVERED', '已签收', 'Delivered', '#52c41a', 'check-circle', 3, false, true, false, true),
('sd-logistics-04', NULL, 'LOGISTICS', 'CANCELLED', '已取消', 'Cancelled', '#ff4d4f', 'close-circle', 4, false, true, false, true);

INSERT INTO sys_status_transition (id, tenant_id, biz_type, from_status, to_status, action_code, action_name, action_name_en, need_approval, enabled, sort_order) VALUES
('st-logistics-01', NULL, 'LOGISTICS', 'CREATED', 'PENDING', 'CONFIRM', '确认', 'Confirm', false, true, 0),
('st-logistics-02', NULL, 'LOGISTICS', 'CREATED', 'CANCELLED', 'CANCEL', '取消', 'Cancel', false, true, 1),
('st-logistics-03', NULL, 'LOGISTICS', 'PENDING', 'IN_TRANSIT', 'DISPATCH', '派送', 'Dispatch', false, true, 0),
('st-logistics-04', NULL, 'LOGISTICS', 'PENDING', 'CANCELLED', 'CANCEL', '取消', 'Cancel', false, true, 1),
('st-logistics-05', NULL, 'LOGISTICS', 'IN_TRANSIT', 'DELIVERED', 'DELIVER', '签收', 'Deliver', false, true, 0);

-- ============================================================
-- 结算单状态 (SETTLEMENT)
-- ============================================================
INSERT INTO sys_status_dict (id, tenant_id, biz_type, status_code, status_name, status_name_en, color, icon, sort_order, is_initial, is_terminal, is_cancellable, enabled) VALUES
('sd-settle-00', NULL, 'SETTLEMENT', 'DRAFT', '草稿', 'Draft', '#d9d9d9', 'file', 0, true, false, true, true),
('sd-settle-01', NULL, 'SETTLEMENT', 'CONFIRMED', '已确认', 'Confirmed', '#52c41a', 'check-circle', 1, false, false, false, true),
('sd-settle-02', NULL, 'SETTLEMENT', 'PARTIAL_PAID', '部分付款', 'Partial Paid', '#faad14', 'credit-card', 2, false, false, false, true),
('sd-settle-03', NULL, 'SETTLEMENT', 'FULLY_PAID', '已付清', 'Fully Paid', '#52c41a', 'smile', 3, false, true, false, true),
('sd-settle-04', NULL, 'SETTLEMENT', 'CLOSED', '已关闭', 'Closed', '#d9d9d9', 'lock', 4, false, true, false, true);

INSERT INTO sys_status_transition (id, tenant_id, biz_type, from_status, to_status, action_code, action_name, action_name_en, need_approval, enabled, sort_order) VALUES
('st-settle-01', NULL, 'SETTLEMENT', 'DRAFT', 'CONFIRMED', 'CONFIRM', '确认', 'Confirm', false, true, 0),
('st-settle-02', NULL, 'SETTLEMENT', 'CONFIRMED', 'PARTIAL_PAID', 'PAY_PARTIAL', '部分付款', 'Partial Pay', false, true, 0),
('st-settle-03', NULL, 'SETTLEMENT', 'CONFIRMED', 'FULLY_PAID', 'PAY_FULL', '全额付款', 'Full Pay', false, true, 1),
('st-settle-04', NULL, 'SETTLEMENT', 'PARTIAL_PAID', 'FULLY_PAID', 'PAY_FULL', '全额付款', 'Full Pay', false, true, 0),
('st-settle-05', NULL, 'SETTLEMENT', 'DRAFT', 'CLOSED', 'CLOSE', '关闭', 'Close', false, true, 1);

-- ============================================================
-- 发票状态 (INVOICE)
-- ============================================================
INSERT INTO sys_status_dict (id, tenant_id, biz_type, status_code, status_name, status_name_en, color, icon, sort_order, is_initial, is_terminal, is_cancellable, enabled) VALUES
('sd-invoice-00', NULL, 'INVOICE', 'DRAFT', '草稿', 'Draft', '#d9d9d9', 'file', 0, true, false, true, true),
('sd-invoice-01', NULL, 'INVOICE', 'ISSUED', '已开票', 'Issued', '#52c41a', 'check-circle', 1, false, false, false, true),
('sd-invoice-02', NULL, 'INVOICE', 'VERIFIED', '已验真', 'Verified', '#1890ff', 'safety-certificate', 2, false, false, false, true),
('sd-invoice-03', NULL, 'INVOICE', 'REJECTED', '已驳回', 'Rejected', '#ff4d4f', 'close-circle', 3, false, true, false, true),
('sd-invoice-04', NULL, 'INVOICE', 'VOIDED', '已作废', 'Voided', '#d9d9d9', 'stop', 4, false, true, false, true);

INSERT INTO sys_status_transition (id, tenant_id, biz_type, from_status, to_status, action_code, action_name, action_name_en, need_approval, enabled, sort_order) VALUES
('st-invoice-01', NULL, 'INVOICE', 'DRAFT', 'ISSUED', 'ISSUE', '开票', 'Issue', false, true, 0),
('st-invoice-02', NULL, 'INVOICE', 'ISSUED', 'VERIFIED', 'VERIFY', '验真', 'Verify', false, true, 0),
('st-invoice-03', NULL, 'INVOICE', 'ISSUED', 'REJECTED', 'REJECT', '驳回', 'Reject', false, true, 1),
('st-invoice-04', NULL, 'INVOICE', 'ISSUED', 'VOIDED', 'VOID', '作废', 'Void', true, true, 2);

-- ============================================================
-- 对账状态 (RECONCILIATION)
-- ============================================================
INSERT INTO sys_status_dict (id, tenant_id, biz_type, status_code, status_name, status_name_en, color, icon, sort_order, is_initial, is_terminal, is_cancellable, enabled) VALUES
('sd-recon-00', NULL, 'RECONCILIATION', 'DRAFT', '草稿', 'Draft', '#d9d9d9', 'file', 0, true, false, true, true),
('sd-recon-01', NULL, 'RECONCILIATION', 'COMPARING', '对账中', 'Comparing', '#1890ff', 'sync', 1, false, false, false, true),
('sd-recon-02', NULL, 'RECONCILIATION', 'MATCHED', '已匹配', 'Matched', '#52c41a', 'check-circle', 2, false, true, false, true),
('sd-recon-03', NULL, 'RECONCILIATION', 'MISMATCHED', '有差异', 'Mismatched', '#faad14', 'warning', 3, false, false, false, true),
('sd-recon-04', NULL, 'RECONCILIATION', 'RESOLVED', '已解决', 'Resolved', '#52c41a', 'smile', 4, false, true, false, true);

INSERT INTO sys_status_transition (id, tenant_id, biz_type, from_status, to_status, action_code, action_name, action_name_en, need_approval, enabled, sort_order) VALUES
('st-recon-01', NULL, 'RECONCILIATION', 'DRAFT', 'COMPARING', 'START', '开始对账', 'Start', false, true, 0),
('st-recon-02', NULL, 'RECONCILIATION', 'COMPARING', 'MATCHED', 'MATCH', '匹配', 'Match', false, true, 0),
('st-recon-03', NULL, 'RECONCILIATION', 'COMPARING', 'MISMATCHED', 'MISMATCH', '有差异', 'Mismatch', false, true, 1),
('st-recon-04', NULL, 'RECONCILIATION', 'MISMATCHED', 'RESOLVED', 'RESOLVE', '解决', 'Resolve', false, true, 0);

-- ============================================================
-- 采购报价状态 (QUOTATION)
-- ============================================================
INSERT INTO sys_status_dict (id, tenant_id, biz_type, status_code, status_name, status_name_en, color, icon, sort_order, is_initial, is_terminal, is_cancellable, enabled) VALUES
('sd-quotation-00', NULL, 'QUOTATION', 'DRAFT', '草稿', 'Draft', '#d9d9d9', 'file', 0, true, false, true, true),
('sd-quotation-01', NULL, 'QUOTATION', 'SUBMITTED', '已提交', 'Submitted', '#1890ff', 'check-circle', 1, false, false, false, true),
('sd-quotation-02', NULL, 'QUOTATION', 'ACCEPTED', '已采纳', 'Accepted', '#52c41a', 'smile', 2, false, true, false, true),
('sd-quotation-03', NULL, 'QUOTATION', 'REJECTED', '未采纳', 'Rejected', '#ff4d4f', 'close-circle', 3, false, true, false, true);

INSERT INTO sys_status_transition (id, tenant_id, biz_type, from_status, to_status, action_code, action_name, action_name_en, need_approval, enabled, sort_order) VALUES
('st-quotation-01', NULL, 'QUOTATION', 'DRAFT', 'SUBMITTED', 'SUBMIT', '提交', 'Submit', false, true, 0),
('st-quotation-02', NULL, 'QUOTATION', 'SUBMITTED', 'ACCEPTED', 'ACCEPT', '采纳', 'Accept', false, true, 0),
('st-quotation-03', NULL, 'QUOTATION', 'SUBMITTED', 'REJECTED', 'REJECT', '未采纳', 'Reject', false, true, 1);

-- ============================================================
-- 采购收货状态 (RECEIPT)
-- ============================================================
INSERT INTO sys_status_dict (id, tenant_id, biz_type, status_code, status_name, status_name_en, color, icon, sort_order, is_initial, is_terminal, is_cancellable, enabled) VALUES
('sd-receipt-00', NULL, 'RECEIPT', 'WAITING', '待收货', 'Waiting', '#faad14', 'clock-circle', 0, true, false, false, true),
('sd-receipt-01', NULL, 'RECEIPT', 'RECEIVING', '收货中', 'Receiving', '#1890ff', 'loading', 1, false, false, false, true),
('sd-receipt-02', NULL, 'RECEIPT', 'INSPECTING', '质检中', 'Inspecting', '#722ed1', 'search', 2, false, false, false, true),
('sd-receipt-03', NULL, 'RECEIPT', 'COMPLETED', '已完成', 'Completed', '#52c41a', 'check-circle', 3, false, true, false, true);

INSERT INTO sys_status_transition (id, tenant_id, biz_type, from_status, to_status, action_code, action_name, action_name_en, need_approval, enabled, sort_order) VALUES
('st-receipt-01', NULL, 'RECEIPT', 'WAITING', 'RECEIVING', 'START', '开始收货', 'Start', false, true, 0),
('st-receipt-02', NULL, 'RECEIPT', 'RECEIVING', 'INSPECTING', 'INSPECT', '质检', 'Inspect', false, true, 0),
('st-receipt-03', NULL, 'RECEIPT', 'INSPECTING', 'COMPLETED', 'COMPLETE', '完成', 'Complete', false, true, 0);

-- ============================================================
-- 供应商结算状态 (SUPPLIER_SETTLEMENT)
-- ============================================================
INSERT INTO sys_status_dict (id, tenant_id, biz_type, status_code, status_name, status_name_en, color, icon, sort_order, is_initial, is_terminal, is_cancellable, enabled) VALUES
('sd-supsettle-00', NULL, 'SUPPLIER_SETTLEMENT', 'DRAFT', '草稿', 'Draft', '#d9d9d9', 'file', 0, true, false, true, true),
('sd-supsettle-01', NULL, 'SUPPLIER_SETTLEMENT', 'CONFIRMED', '已确认', 'Confirmed', '#52c41a', 'check-circle', 1, false, false, false, true),
('sd-supsettle-02', NULL, 'SUPPLIER_SETTLEMENT', 'PARTIAL_PAID', '部分付款', 'Partial Paid', '#faad14', 'credit-card', 2, false, false, false, true),
('sd-supsettle-03', NULL, 'SUPPLIER_SETTLEMENT', 'FULLY_PAID', '已付清', 'Fully Paid', '#52c41a', 'smile', 3, false, true, false, true),
('sd-supsettle-04', NULL, 'SUPPLIER_SETTLEMENT', 'CLOSED', '已关闭', 'Closed', '#d9d9d9', 'lock', 4, false, true, false, true);

INSERT INTO sys_status_transition (id, tenant_id, biz_type, from_status, to_status, action_code, action_name, action_name_en, need_approval, enabled, sort_order) VALUES
('st-supsettle-01', NULL, 'SUPPLIER_SETTLEMENT', 'DRAFT', 'CONFIRMED', 'CONFIRM', '确认', 'Confirm', false, true, 0),
('st-supsettle-02', NULL, 'SUPPLIER_SETTLEMENT', 'CONFIRMED', 'PARTIAL_PAID', 'PAY_PARTIAL', '部分付款', 'Partial Pay', false, true, 0),
('st-supsettle-03', NULL, 'SUPPLIER_SETTLEMENT', 'CONFIRMED', 'FULLY_PAID', 'PAY_FULL', '全额付款', 'Full Pay', false, true, 1),
('st-supsettle-04', NULL, 'SUPPLIER_SETTLEMENT', 'PARTIAL_PAID', 'FULLY_PAID', 'PAY_FULL', '全额付款', 'Full Pay', false, true, 0),
('st-supsettle-05', NULL, 'SUPPLIER_SETTLEMENT', 'DRAFT', 'CLOSED', 'CLOSE', '关闭', 'Close', false, true, 1);

-- ============================================================
-- 通知模板状态 (NOTIFICATION)
-- ============================================================
INSERT INTO sys_status_dict (id, tenant_id, biz_type, status_code, status_name, status_name_en, color, icon, sort_order, is_initial, is_terminal, is_cancellable, enabled) VALUES
('sd-notify-00', NULL, 'NOTIFICATION', 'DISABLED', '已禁用', 'Disabled', '#d9d9d9', 'pause-circle', 0, false, false, false, true),
('sd-notify-01', NULL, 'NOTIFICATION', 'ENABLED', '已启用', 'Enabled', '#52c41a', 'check-circle', 1, true, false, false, true);

INSERT INTO sys_status_transition (id, tenant_id, biz_type, from_status, to_status, action_code, action_name, action_name_en, need_approval, enabled, sort_order) VALUES
('st-notify-01', NULL, 'NOTIFICATION', 'DISABLED', 'ENABLED', 'ENABLE', '启用', 'Enable', false, true, 0),
('st-notify-02', NULL, 'NOTIFICATION', 'ENABLED', 'DISABLED', 'DISABLE', '禁用', 'Disable', false, true, 0);

-- ============================================================
-- 字典类型初始数据
-- ============================================================
INSERT INTO sys_dict_type (id, tenant_id, dict_code, dict_name, dict_name_en, description, status, is_system, sort_order) VALUES
('dt-001', NULL, 'currency', '币种', 'Currency', '货币类型', 1, true, 0),
('dt-002', NULL, 'unit', '计量单位', 'Unit of Measure', '商品计量单位', 1, true, 1),
('dt-003', NULL, 'warehouse_type', '仓库类型', 'Warehouse Type', '仓库分类', 1, true, 2),
('dt-004', NULL, 'payment_method', '支付方式', 'Payment Method', '支付渠道', 1, true, 3),
('dt-005', NULL, 'notify_channel', '通知渠道', 'Notification Channel', '消息通知渠道', 1, true, 4),
('dt-006', NULL, 'country', '国家/地区', 'Country/Region', '国家和地区编码', 1, true, 5),
('dt-007', NULL, 'product_category', '产品分类', 'Product Category', '商品分类', 1, false, 6);

-- 字典项初始数据
INSERT INTO sys_dict_item (id, tenant_id, dict_type_id, dict_code, item_code, item_name, item_name_en, sort_order, is_default, status) VALUES
-- 币种
('di-001', NULL, 'dt-001', 'currency', 'CNY', '人民币', 'Chinese Yuan', 0, true, 1),
('di-002', NULL, 'dt-001', 'currency', 'USD', '美元', 'US Dollar', 1, false, 1),
('di-003', NULL, 'dt-001', 'currency', 'EUR', '欧元', 'Euro', 2, false, 1),
('di-004', NULL, 'dt-001', 'currency', 'JPY', '日元', 'Japanese Yen', 3, false, 1),
-- 计量单位
('di-010', NULL, 'dt-002', 'unit', 'PCS', '件', 'Pieces', 0, true, 1),
('di-011', NULL, 'dt-002', 'unit', 'KG', '千克', 'Kilogram', 1, false, 1),
('di-012', NULL, 'dt-002', 'unit', 'BOX', '箱', 'Box', 2, false, 1),
('di-013', NULL, 'dt-002', 'unit', 'SET', '套', 'Set', 3, false, 1),
-- 仓库类型
('di-020', NULL, 'dt-003', 'warehouse_type', 'NORMAL', '普通仓库', 'Normal', 0, true, 1),
('di-021', NULL, 'dt-003', 'warehouse_type', 'COLD', '冷链仓库', 'Cold Storage', 1, false, 1),
('di-022', NULL, 'dt-003', 'warehouse_type', 'HAZARDOUS', '危险品仓库', 'Hazardous', 2, false, 1),
-- 支付方式
('di-030', NULL, 'dt-004', 'payment_method', 'ALIPAY', '支付宝', 'Alipay', 0, true, 1),
('di-031', NULL, 'dt-004', 'payment_method', 'WECHAT', '微信支付', 'WeChat Pay', 1, false, 1),
('di-032', NULL, 'dt-004', 'payment_method', 'BANK', '银行转账', 'Bank Transfer', 2, false, 1),
('di-033', NULL, 'dt-004', 'payment_method', 'CREDIT', '信用支付', 'Credit', 3, false, 1),
-- 通知渠道
('di-040', NULL, 'dt-005', 'notify_channel', 'EMAIL', '邮件', 'Email', 0, true, 1),
('di-041', NULL, 'dt-005', 'notify_channel', 'SMS', '短信', 'SMS', 1, false, 1),
('di-042', NULL, 'dt-005', 'notify_channel', 'DINGTALK', '钉钉', 'DingTalk', 2, false, 1),
('di-043', NULL, 'dt-005', 'notify_channel', 'WECOM', '企业微信', 'WeCom', 3, false, 1),
('di-044', NULL, 'dt-005', 'notify_channel', 'WEBHOOK', 'Webhook', 'Webhook', 4, false, 1);
