package com.scmcloud.common.data.rw.tracing;

import com.scmcloud.common.data.rw.routing.ReadWriteRoutingContext;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * 读写分离链路追踪拦截�?
 * <p>
 * 集成 OpenTelemetry，记录：
 * - 路由类型（MASTER/SLAVE�?
 * - 目标数据�?
 * - 执行耗时
 * - 是否降级
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
public class ReadWriteTracingInterceptor implements MethodInterceptor {
    private static final String INSTRUMENTATION_NAME = "com.frog.data.rw";

    private final Tracer tracer;

    public ReadWriteTracingInterceptor() {
        this.tracer = GlobalOpenTelemetry.getTracer(INSTRUMENTATION_NAME);
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        String methodName = invocation.getMethod().getDeclaringClass().getSimpleName() +
                "." + invocation.getMethod().getName();

        ReadWriteRoutingContext.RoutingType routingType = ReadWriteRoutingContext.current();

        Span span = tracer.spanBuilder("db.rw." + methodName)
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("db.rw.routing_type", routingType.name())
                .setAttribute("db.rw.force_master", ReadWriteRoutingContext.isForceMaster())
                .startSpan();

        try (Scope ignored = span.makeCurrent()) {
            // 记录指定的从�?
            String specifiedSlave = ReadWriteRoutingContext.getSpecifiedSlave();
            if (specifiedSlave != null) {
                span.setAttribute("db.rw.specified_slave", specifiedSlave);
            }

            // 执行方法
            Object result = invocation.proceed();

            // 记录成功
            span.setStatus(StatusCode.OK);
            return result;

        } catch (Exception e) {
            // 记录异常
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;

        } finally {
            span.end();
        }
    }
}
