package com.scmcloud.inventory.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.inventory.domain.entity.InvAlert;
import com.scmcloud.inventory.mapper.InvAlertMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvAlertCommandService {

    private final InvAlertMapper alertMapper;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int save(InvAlert alert) {
        return alertMapper.insert(alert);
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int updateById(InvAlert alert) {
        return alertMapper.updateById(alert);
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public int removeById(String id) {
        return alertMapper.deleteById(id);
    }
}
