package com.scmcloud.inventory.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.inventory.domain.entity.InvSnapshot;
import com.scmcloud.inventory.mapper.InvSnapshotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvSnapshotCommandService {

    private final InvSnapshotMapper snapshotMapper;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int save(InvSnapshot snapshot) {
        return snapshotMapper.insert(snapshot);
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int updateById(InvSnapshot snapshot) {
        return snapshotMapper.updateById(snapshot);
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int removeById(String id) {
        return snapshotMapper.deleteById(id);
    }
}
