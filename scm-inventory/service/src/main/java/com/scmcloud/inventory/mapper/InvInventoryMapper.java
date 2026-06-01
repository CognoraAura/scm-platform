package com.scmcloud.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.inventory.domain.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存�?Mapper
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Mapper
public interface InvInventoryMapper extends BaseMapper<Inventory> {
}