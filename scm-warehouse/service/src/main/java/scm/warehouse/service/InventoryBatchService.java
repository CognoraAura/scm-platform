package scm.warehouse.service;

import scm.warehouse.job.NearExpiryAlertJob;
import scm.warehouse.vo.NearExpiryProductVO;
import java.util.List;
import java.util.UUID;

public interface InventoryBatchService {
    List<NearExpiryProductVO> getNearExpiryProducts(UUID tenantId, NearExpiryAlertJob.AlertLevel alertLevel);
}
