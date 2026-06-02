package com.scmcloud.common.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.common.log.entity.SysAuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 操作审计日志�Mapper 接口
 * </p>
 *
 * @author author
 * @since 2025-10-14
 */
@Mapper
public interface SysAuditLogMapper extends BaseMapper<SysAuditLog> {

}
