package com.scmcloud.purchase.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.purchase.domain.entity.PurRfq;
import com.scmcloud.purchase.mapper.PurRfqMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurRfqCommandService {

    private final PurRfqMapper purRfqMapper;

    @Master(reason = "保存询价单")
    @Transactional(rollbackFor = Exception.class)
    public boolean save(PurRfq entity) {
        return purRfqMapper.insert(entity) > 0;
    }

    @Master(reason = "更新询价单")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(PurRfq entity) {
        return purRfqMapper.updateById(entity) > 0;
    }

    @Master(reason = "删除询价单")
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(String id) {
        return purRfqMapper.deleteById(id) > 0;
    }

    @Master(reason = "发布询价单")
    @Transactional(rollbackFor = Exception.class)
    public boolean publish(String id) {
        PurRfq rfq = purRfqMapper.selectById(id);
        if (rfq == null || rfq.getDeleted()) {
            throw new IllegalArgumentException("询价单不存在: " + id);
        }
        if (rfq.getStatus() != 0) {
            throw new IllegalStateException("只有草稿状态的询价单才能发布");
        }
        rfq.setStatus(1);
        rfq.setUpdateTime(LocalDateTime.now());
        return purRfqMapper.updateById(rfq) > 0;
    }

    @Master(reason = "关闭询价单")
    @Transactional(rollbackFor = Exception.class)
    public boolean close(String id) {
        PurRfq rfq = purRfqMapper.selectById(id);
        if (rfq == null || rfq.getDeleted()) {
            throw new IllegalArgumentException("询价单不存在: " + id);
        }
        if (rfq.getStatus() == 4) {
            throw new IllegalStateException("询价单已关闭");
        }
        rfq.setStatus(4);
        rfq.setUpdateTime(LocalDateTime.now());
        return purRfqMapper.updateById(rfq) > 0;
    }
}
