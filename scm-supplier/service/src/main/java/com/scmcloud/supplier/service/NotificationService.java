package com.scmcloud.supplier.service;

import com.scmcloud.supplier.vo.OverdueApprovalTaskVO;

public interface NotificationService {
    void sendApprovalReminder(OverdueApprovalTaskVO task);
}
