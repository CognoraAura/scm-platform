package com.scmcloud.inventory.service.impl;

import com.scmcloud.inventory.domain.entity.InvSnapshot;
import com.scmcloud.inventory.mapper.InvSnapshotMapper;
import com.scmcloud.inventory.service.IInvSnapshotService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 库存快照表（每日快照�?服务实现�?
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Service
public class InvSnapshotServiceImpl extends ServiceImpl<InvSnapshotMapper, InvSnapshot> implements IInvSnapshotService {

}
