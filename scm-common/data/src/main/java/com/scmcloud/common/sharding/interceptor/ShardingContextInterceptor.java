package com.scmcloud.common.sharding.interceptor;

import com.scmcloud.common.web.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.infra.hint.HintManager;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 安全地管�HintManager（防止嵌�/ 泄露�
 *
 * @author Deng
 * createData 2025/11/11 15:16
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class ShardingContextInterceptor implements HandlerInterceptor {

    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        UUID userId = SecurityUtils.getCurrentUserUuid().orElse(null);
        if (userId != null) {
            HintManager.clear(); // 确保干净上下�
            HintManager hintManager = HintManager.getInstance();
            hintManager.addDatabaseShardingValue("sys_user", userId);
            hintManager.addTableShardingValue("sys_audit_log", LocalDateTime.now());
        }
        return true;
    }

    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                @NonNull Object handler, Exception ex) {
        HintManager.clear();
    }
}

