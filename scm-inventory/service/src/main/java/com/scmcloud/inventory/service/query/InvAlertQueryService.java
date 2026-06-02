package com.scmcloud.inventory.service.query;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.inventory.domain.entity.InvAlert;
import com.scmcloud.inventory.mapper.InvAlertMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InvAlertQueryService {

    private final InvAlertMapper alertMapper;

    @Slave
    public InvAlert getById(String id) {
        return alertMapper.selectById(id);
    }

    @Slave
    public List<InvAlert> list() {
        return alertMapper.selectList(Wrappers.emptyWrapper());
    }

    @Slave
    public Page<InvAlert> page(int page, int size) {
        return alertMapper.selectPage(new Page<>(page, size), Wrappers.emptyWrapper());
    }

    @Slave
    public long count() {
        return alertMapper.selectCount(Wrappers.emptyWrapper());
    }
}
