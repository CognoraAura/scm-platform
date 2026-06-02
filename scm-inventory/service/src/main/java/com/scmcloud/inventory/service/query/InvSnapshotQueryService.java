package com.scmcloud.inventory.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.inventory.domain.entity.InvSnapshot;
import com.scmcloud.inventory.mapper.InvSnapshotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InvSnapshotQueryService {

    private final InvSnapshotMapper snapshotMapper;

    @Slave
    public InvSnapshot getById(String id) {
        return snapshotMapper.selectById(id);
    }

    @Slave
    public List<InvSnapshot> list() {
        return snapshotMapper.selectList(Wrappers.emptyWrapper());
    }

    @Slave
    public Page<InvSnapshot> page(int page, int size) {
        return snapshotMapper.selectPage(new Page<>(page, size), Wrappers.emptyWrapper());
    }

    @Slave
    public long count() {
        return snapshotMapper.selectCount(Wrappers.emptyWrapper());
    }
}
