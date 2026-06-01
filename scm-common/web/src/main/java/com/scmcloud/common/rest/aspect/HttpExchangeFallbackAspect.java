package com.scmcloud.common.rest.aspect;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import java.lang.reflect.Method;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

/**
 * @HttpExchange 降级处理 AOP 切面
 * <p>替代 OpenFeign �?FallbackFactory</p>
 *
 * <p>功能�?
 * <ul>
 *   <li>拦截所�?@SentinelResource 注解的方�?/li>
 *   <li>异常分类：BlockException（限流）、TimeoutException（超时）、其他异�?/li>
 *   <li>自动调用接口�?default 降级方法</li>
 *   <li>Micrometer 指标跟踪</li>
 * </ul>
 *
 * <p>使用示例�?
 * <pre>
 * {@code
 * @HttpExchange("/api/users")
 * public interface UserServiceClient {
 *     @GetExchange("/{id}")
 *     @SentinelResource(value = "user-service:getById", fallback = "getByIdFallback")
 *     ApiResponse<User> getById(@PathVariable Long id);
 *
 *     // 降级方法（default�?
 *     default ApiResponse<User> getByIdFallback(Long id, Throwable ex) {
 *         log.warn("User service fallback: id={}", id);
 *         return ApiResponse.fail(503, "Service unavailable");
 *     }
 * }
 * }
 * </pre>
 *
 * @author Claude
 * @since 2025-12-29
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class HttpExchangeFallbackAspect {
    private final MeterRegistry meterRegistry;

    /**
     * 拦截所�?@SentinelResource 注解的方�?
     */
    @Around("@annotation(sentinelResource)")
    public Object handleSentinelResource(
        ProceedingJoinPoint joinPoint,
        SentinelResource sentinelResource) throws Throwable {

        String resourceName = sentinelResource.value();
        String fallbackMethod = sentinelResource.fallback();

        try {
            // 执行原方�?
            Object result = joinPoint.proceed();

            // 记录成功指标
            meterRegistry.counter("http.exchange.success",
                                  "resource", resourceName).increment();

            return result;

        } catch (BlockException ex) {
            // Sentinel 限流/降级
            meterRegistry.counter("http.exchange.blocked",
                                  "resource", resourceName).increment();
            log.warn("HTTP Exchange blocked by Sentinel: resource={}, reason={}",
                     resourceName, ex.getRule());

            return invokeFallback(joinPoint, fallbackMethod, ex);

        } catch (ResourceAccessException | SocketTimeoutException | TimeoutException ex) {
            // 超时异常
            meterRegistry.counter("http.exchange.timeout",
                                  "resource", resourceName).increment();
            log.error("HTTP Exchange timeout: resource={}, error={}",
                      resourceName, ex.getMessage());

            return invokeFallback(joinPoint, fallbackMethod, ex);

        } catch (Exception ex) {
            // 其他异常
            meterRegistry.counter("http.exchange.failure",
                                  "resource", resourceName,
                                  "exception", ex.getClass().getSimpleName()).increment();
            log.error("HTTP Exchange failed: resource={}, exception={}",
                      resourceName, ex.getClass().getSimpleName(), ex);

            return invokeFallback(joinPoint, fallbackMethod, ex);
        }
    }

    /**
     * 调用降级方法
     *
     * <p>降级方法签名�?
     * <ul>
     *   <li>参数：与原方法相�?+ Throwable ex</li>
     *   <li>返回值：与原方法相同</li>
     *   <li>修饰符：default（接口默认方法）</li>
     * </ul>
     *
     * @param joinPoint 切点
     * @param fallbackMethodName 降级方法�?
     * @param cause 异常
     * @return 降级结果
     */
    private Object invokeFallback(
        ProceedingJoinPoint joinPoint,
        String fallbackMethodName,
        Throwable cause) throws Throwable {

        if (fallbackMethodName == null || fallbackMethodName.isEmpty()) {
            // 无降级方法，直接抛出异常
            throw cause;
        }

        Object target = joinPoint.getTarget();
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        // 查找降级方法
        Method fallbackMethod = findFallbackMethod(
            signature.getDeclaringType(),
            fallbackMethodName,
            args
        );

        if (fallbackMethod != null) {
            try {
                // 调用降级方法（添�?Throwable 参数�?
                Object[] fallbackArgs = buildFallbackArgs(args, cause);
                fallbackMethod.setAccessible(true);
                return fallbackMethod.invoke(target, fallbackArgs);
            } catch (Exception e) {
                log.error("Failed to invoke fallback method '{}': {}",
                          fallbackMethodName, e.getMessage(), e);
                throw cause;
            }
        } else {
            log.warn("Fallback method '{}' not found, rethrowing exception", fallbackMethodName);
            throw cause;
        }
    }

    /**
     * 查找降级方法
     *
     * <p>查找策略�?
     * <ol>
     *   <li>查找接口�?default 方法</li>
     *   <li>参数签名：原方法参数 + Throwable</li>
     * </ol>
     *
     * @param declaringType 声明类型
     * @param methodName 方法�?
     * @param originalArgs 原始参数
     * @return 降级方法，未找到则返�?null
     */
    private Method findFallbackMethod(
        Class<?> declaringType,
        String methodName,
        Object[] originalArgs) {

        try {
            // 构建参数类型数组（原参数 + Throwable�?
            Class<?>[] paramTypes = new Class[originalArgs.length + 1];
            for (int i = 0; i < originalArgs.length; i++) {
                paramTypes[i] = originalArgs[i] != null ?
                    originalArgs[i].getClass() : Object.class;
            }
            paramTypes[originalArgs.length] = Throwable.class;

            // 在声明接口中查找 default 方法
            return declaringType.getDeclaredMethod(methodName, paramTypes);

        } catch (NoSuchMethodException e) {
            // 尝试松散匹配（参数类型可能不完全匹配�?
            for (Method method : declaringType.getDeclaredMethods()) {
                if (method.getName().equals(methodName) &&
                    method.isDefault() &&
                    method.getParameterCount() == originalArgs.length + 1) {

                    // 检查最后一个参数是否为 Throwable
                    Class<?>[] types = method.getParameterTypes();
                    if (Throwable.class.isAssignableFrom(types[types.length - 1])) {
                        return method;
                    }
                }
            }

            log.debug("Fallback method '{}' not found in {}", methodName, declaringType.getName());
            return null;
        }
    }

    /**
     * 构建降级方法参数（添�?Throwable�?
     *
     * @param originalArgs 原始参数
     * @param cause 异常
     * @return 降级方法参数数组
     */
    private Object[] buildFallbackArgs(Object[] originalArgs, Throwable cause) {
        Object[] fallbackArgs = new Object[originalArgs.length + 1];
        System.arraycopy(originalArgs, 0, fallbackArgs, 0, originalArgs.length);
        fallbackArgs[originalArgs.length] = cause;
        return fallbackArgs;
    }
}
