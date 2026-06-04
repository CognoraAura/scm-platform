package com.scmcloud.common.lock;

import com.scmcloud.common.exception.BusinessException;
import com.scmcloud.common.exception.ErrorCode;
import com.scmcloud.common.redis.lock.DistributedLock;
import com.scmcloud.common.redis.lock.LockHandle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * AOP aspect for @DistributedLock annotation.
 * Automatically acquires and releases distributed locks around method execution.
 *
 * @author SCM Platform
 * @since 2026-06-04
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final DistributedLock distributedLock;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(com.scmcloud.common.lock.DistributedLockAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLockAnnotation annotation = method.getAnnotation(DistributedLockAnnotation.class);

        // Parse lock key from SpEL
        String lockKey = parseKey(annotation.key(), method, joinPoint.getArgs());

        // Try to acquire lock
        LockHandle lockHandle;
        if (annotation.blocking()) {
            lockHandle = distributedLock.tryLock(lockKey, annotation.waitTime(), annotation.ttl(), annotation.unit());
        } else {
            lockHandle = distributedLock.tryLock(lockKey, annotation.ttl(), annotation.unit());
        }

        if (lockHandle == null) {
            log.warn("Failed to acquire distributed lock: key={}, method={}",
                    lockKey, method.getName());
            throw new BusinessException(ErrorCode.LOCK_ACQUISITION_FAILED, annotation.errorMessage());
        }

        try {
            log.debug("Acquired distributed lock: key={}", lockKey);
            return joinPoint.proceed();
        } finally {
            lockHandle.unlock();
            log.debug("Released distributed lock: key={}", lockKey);
        }
    }

    private String parseKey(String keyExpression, Method method, Object[] args) {
        try {
            EvaluationContext context = new StandardEvaluationContext();
            String[] paramNames = nameDiscoverer.getParameterNames(method);
            if (paramNames != null) {
                for (int i = 0; i < paramNames.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }
            return parser.parseExpression(keyExpression).getValue(context, String.class);
        } catch (Exception e) {
            log.warn("Failed to parse lock key expression '{}': {}", keyExpression, e.getMessage());
            return keyExpression;
        }
    }
}
