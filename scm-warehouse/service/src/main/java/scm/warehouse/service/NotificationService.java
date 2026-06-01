package scm.warehouse.service;

import scm.warehouse.job.NearExpiryAlertJob;
import scm.warehouse.vo.NearExpiryProductVO;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface NotificationService {
    void sendNearExpiryAlert(UUID tenantId, List<NearExpiryProductVO> products, Map<NearExpiryAlertJob.AlertLevel, Long> alertCounts);
}
