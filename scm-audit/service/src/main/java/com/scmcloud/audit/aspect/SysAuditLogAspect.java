package com.scmcloud.audit.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scmcloud.audit.domain.entity.SysAuditLog;
import com.scmcloud.audit.mapper.SysAuditLogMapper;
import com.scmcloud.common.log.annotation.AuditLog;
import com.scmcloud.common.status.StatusValidator;
import com.scmcloud.common.security.util.DesensitizeUtils;
import com.scmcloud.common.security.util.IpUtils;
import com.scmcloud.common.web.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class SysAuditLogAspect {
    private final SysAuditLogMapper sysAuditLogMapper;
    private final ObjectMapper objectMapper;
    private final StatusValidator statusValidator;

    @Around("@annotation(com.scmcloud.common.log.annotation.AuditLog)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();

        MethodSignature signature = (MethodSignature) point.getSignature();
        AuditLog annotation = signature.getMethod().getAnnotation(AuditLog.class);

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        UUID userUuid = SecurityUtils.getCurrentUserUuid().orElse(null);
        SysAuditLog auditLog = SysAuditLog.builder()
                .userId(userUuid != null ? userUuid.toString() : null)
                .username(SecurityUtils.getCurrentUsername().orElse(null))
                .operationType(annotation.businessType())
                .operationDesc(annotation.operation())
                .riskLevel(annotation.riskLevel())
                .build();

        if (request != null) {
            auditLog.setRequestUri(request.getRequestURI());
            auditLog.setRequestMethod(request.getMethod());
            auditLog.setIpAddress(IpUtils.getClientIp(request));
            auditLog.setUserAgent(request.getHeader("User-Agent"));

            if (annotation.recordParams()) {
                try {
                    Object[] args = point.getArgs();
                    String params = objectMapper.writeValueAsString(args);
                    params = DesensitizeUtils.desensitize(params);
                    auditLog.setRequestParams(params);
                } catch (Exception e) {
                    log.error("Failed to serialize request params", e);
                }
            }
        }

        Object result;
        try {
            result = point.proceed();
            auditLog.setStatus(1);

            if (annotation.recordResult() && result != null) {
                try {
                    String response = objectMapper.writeValueAsString(result);
                    auditLog.setResponseData(DesensitizeUtils.desensitize(response));
                } catch (Exception e) {
                    log.error("Failed to serialize response", e);
                }
            }

        } catch (Exception e) {
            auditLog.setStatus(0);
            auditLog.setErrorMsg(e.getMessage());
            throw e;
        } finally {
            long executeTime = System.currentTimeMillis() - startTime;
            auditLog.setExecuteTime((int) executeTime);
            auditLog.setCreateTime(LocalDateTime.now());

            try {
                sysAuditLogMapper.insert(auditLog);
            } catch (Exception e) {
                log.error("Failed to save audit log", e);
            }
        }

        return result;
    }
}
