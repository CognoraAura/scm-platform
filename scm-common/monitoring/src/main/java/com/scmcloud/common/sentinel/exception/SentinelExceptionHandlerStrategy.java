package com.scmcloud.common.sentinel.exception;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.scmcloud.common.response.ApiResponse;

/**
 * Sentinel 异常处理器策略接�
 *
 * @author Deng
 * createData 2025/10/20 10:39
 * @version 1.0
 */
public interface SentinelExceptionHandlerStrategy {
    /**
     * 判断是否支持处理该异�
     * @param ex 异常
     * @return 是否支持
     */
    boolean supports(BlockException ex);
    
    /**
     * 处理异常
     * @param ex 异常
     * @return 响应结果
     */
    ApiResponse<Void> handle(BlockException ex);
}