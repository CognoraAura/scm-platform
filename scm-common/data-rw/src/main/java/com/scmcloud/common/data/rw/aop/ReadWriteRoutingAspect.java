package com.scmcloud.common.data.rw.aop;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.common.data.rw.routing.ReadWriteRoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * 璇诲啓鍒嗙璺敱鍒囬潰
 * <p>
 * 澶勭悊 @Master銆丂Slave 娉ㄨВ锛屼互鍙婅嚜鍔ㄥ垽鏂簨鍔＄被锟?
 * <p>
 * 浼樺厛绾ч珮锟紷Transactional锛岀‘淇濆湪浜嬪姟寮€鍚墠璁剧疆璺敱
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReadWriteRoutingAspect {
    @Pointcut("@annotation(com.scmcloud.common.data.rw.annotation.Master)")
    public void masterPointcut() {}

    @Pointcut("@annotation(com.scmcloud.common.data.rw.annotation.Slave)")
    public void slavePointcut() {}

    @Pointcut("@within(com.scmcloud.common.data.rw.annotation.Master)")
    public void masterClassPointcut() {}

    @Pointcut("@within(com.scmcloud.common.data.rw.annotation.Slave)")
    public void slaveClassPointcut() {}

    @Pointcut("@annotation(org.springframework.transaction.annotation.Transactional)")
    public void transactionalPointcut() {}

    /**
     * 澶勭悊 @Master 娉ㄨВ
     */
    @Around("masterPointcut() || masterClassPointcut()")
    public Object aroundMaster(ProceedingJoinPoint joinPoint) throws Throwable {
        Master master = getAnnotation(joinPoint, Master.class);
        String reason = master != null ? master.reason() : "";

        log.debug("[RW-Routing] @Master intercepted: {}.{}, reason: {}",
                joinPoint.getTarget().getClass().getSimpleName(),
                joinPoint.getSignature().getName(),
                reason);

        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.MASTER);
        try {
            return joinPoint.proceed();
        } finally {
            ReadWriteRoutingContext.pop();
        }
    }

    /**
     * 澶勭悊 @Slave 娉ㄨВ
     */
    @Around("slavePointcut() || slaveClassPointcut()")
    public Object aroundSlave(ProceedingJoinPoint joinPoint) throws Throwable {
        Slave slave = getAnnotation(joinPoint, Slave.class);

        String slaveName = slave != null ? slave.value() : "";
        if (!slaveName.isEmpty()) {
            ReadWriteRoutingContext.specifySlave(slaveName);
        }

        log.debug("[RW-Routing] @Slave intercepted: {}.{}, slave: {}",
                joinPoint.getTarget().getClass().getSimpleName(),
                joinPoint.getSignature().getName(),
                slaveName.isEmpty() ? "auto" : slaveName);

        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE);
        try {
            return joinPoint.proceed();
        } finally {
            ReadWriteRoutingContext.pop();
        }
    }

    /**
     * 澶勭悊 @Transactional 娉ㄨВ
     * <p>
     * - readOnly=true 锟戒粠搴?
     * - readOnly=false 锟戒富搴?
     */
    @Around("transactionalPointcut()")
    public Object aroundTransactional(ProceedingJoinPoint joinPoint) throws Throwable {
        Transactional transactional = getAnnotation(joinPoint, Transactional.class);

        // 濡傛灉宸茬粡鏈夋樉寮忚矾鐢憋紝涓嶅啀澶勭悊
        if (ReadWriteRoutingContext.current() != ReadWriteRoutingContext.RoutingType.AUTO) {
            return joinPoint.proceed();
        }

        if (transactional != null && transactional.readOnly()) {
            log.debug("[RW-Routing] @Transactional(readOnly=true) intercepted: {}.{}",
                    joinPoint.getTarget().getClass().getSimpleName(),
                    joinPoint.getSignature().getName());

            ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE);
            try {
                return joinPoint.proceed();
            } finally {
                ReadWriteRoutingContext.pop();
            }
        } else {
            // 鍐欎簨鍔★紝璁板綍鍐欐搷浣滄椂锟?
            ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.MASTER);
            try {
                Object result = joinPoint.proceed();
                // 浜嬪姟鎴愬姛鍚庢爣璁板啓鎿嶄綔
                ReadWriteRoutingContext.markWrite();
                return result;
            } finally {
                ReadWriteRoutingContext.pop();
            }
        }
    }

    private <T extends Annotation> T getAnnotation(ProceedingJoinPoint joinPoint, Class<T> annotationClass) {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 鍏堜粠鏂规硶涓婃壘
        T annotation = method.getAnnotation(annotationClass);
        if (annotation != null) {
            return annotation;
        }

        // 鍐嶄粠绫讳笂锟?
        return joinPoint.getTarget().getClass().getAnnotation(annotationClass);
    }
}
