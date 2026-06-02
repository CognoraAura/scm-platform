package com.scmcloud.finance.service.query;

import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.finance.domain.entity.ReconciliationRecord;
import com.scmcloud.finance.mapper.ReconciliationRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationRecordQueryService {
    private final ReconciliationRecordMapper reconciliationRecordMapper;

    @Slave
    public ReconciliationRecord getById(String id) {
        return reconciliationRecordMapper.selectById(id);
    }
}
