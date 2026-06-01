package scm.supplier.service;

import scm.supplier.vo.OverdueApprovalTaskVO;

public interface NotificationService {
    void sendApprovalReminder(OverdueApprovalTaskVO task);
}
