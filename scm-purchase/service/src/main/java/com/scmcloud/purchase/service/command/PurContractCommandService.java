package com.scmcloud.purchase.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.purchase.domain.entity.PurContract;
import com.scmcloud.purchase.mapper.PurContractMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurContractCommandService {

    private final PurContractMapper purContractMapper;

    @Master(reason = "保存合同")
    @Transactional(rollbackFor = Exception.class)
    public boolean save(PurContract entity) {
        return purContractMapper.insert(entity) > 0;
    }

    @Master(reason = "更新合同")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(PurContract entity) {
        return purContractMapper.updateById(entity) > 0;
    }

    @Master(reason = "删除合同")
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(String id) {
        return purContractMapper.deleteById(id) > 0;
    }

    @Master(reason = "签署合同")
    @Transactional(rollbackFor = Exception.class)
    public boolean sign(String id, String signedBy, String signedByName) {
        PurContract contract = purContractMapper.selectById(id);
        if (contract == null || contract.getDeleted()) {
            throw new IllegalArgumentException("合同不存在: " + id);
        }
        if (contract.getStatus() != 1) {
            throw new IllegalStateException("只有待签署的合同才能签署");
        }
        contract.setStatus(2);
        contract.setSignedBy(signedBy);
        contract.setSignedByName(signedByName);
        contract.setSignedAt(LocalDateTime.now());
        contract.setUpdateTime(LocalDateTime.now());
        return purContractMapper.updateById(contract) > 0;
    }

    @Master(reason = "终止合同")
    @Transactional(rollbackFor = Exception.class)
    public boolean terminate(String id) {
        PurContract contract = purContractMapper.selectById(id);
        if (contract == null || contract.getDeleted()) {
            throw new IllegalArgumentException("合同不存在: " + id);
        }
        if (contract.getStatus() == 4) {
            throw new IllegalStateException("合同已终止");
        }
        if (contract.getStatus() == 0) {
            throw new IllegalStateException("草稿合同不能终止，请直接删除");
        }
        contract.setStatus(4);
        contract.setUpdateTime(LocalDateTime.now());
        return purContractMapper.updateById(contract) > 0;
    }
}
