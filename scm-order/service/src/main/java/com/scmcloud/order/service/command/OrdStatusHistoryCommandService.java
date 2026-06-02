package com.scmcloud.order.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.order.domain.entity.OrdStatusHistory;
import com.scmcloud.order.mapper.OrdStatusHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrdStatusHistoryCommandService {

    private final OrdStatusHistoryMapper ordStatusHistoryMapper;

    @Master(reason = "创建状态历史")
    @Transactional(rollbackFor = Exception.class)
    public int save(OrdStatusHistory history) {
        return ordStatusHistoryMapper.insert(history);
    }

    @Master(reason = "删除状态历史")
    @Transactional(rollbackFor = Exception.class)
    public int removeById(String id) {
        return ordStatusHistoryMapper.deleteById(id);
    }
}
