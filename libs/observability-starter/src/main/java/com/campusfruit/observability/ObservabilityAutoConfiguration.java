package com.campusfruit.observability;

import io.micrometer.tracing.Tracer;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * 可观测性自动配置
 * <p>
 * 配置日志 MDC 中的 traceId / spanId，并暴露 OpenTelemetry 相关的 Bean。
 */
@AutoConfiguration
@ConditionalOnClass(Tracer.class)
public class ObservabilityAutoConfiguration {

    /**
     * 注册一个 Tracer 感知的 Bean，用于在日志 MDC 中设置 traceId 和 spanId。
     * Micrometer Tracing 在 1.4+ 中默认通过 Observation 机制自动向 MDC 写入 traceId/spanId，
     * 此配置确保即使在非 Observation 路径下也能正确传播。
     */
    @Bean
    public ObservabilityMdcInitializer observabilityMdcInitializer(Tracer tracer) {
        return new ObservabilityMdcInitializer(tracer);
    }

    /**
     * 内部类：负责在需要时向 MDC 写入追踪标识。
     */
    public static class ObservabilityMdcInitializer {

        private static final String TRACE_ID_KEY = "traceId";
        private static final String SPAN_ID_KEY = "spanId";

        private final Tracer tracer;

        public ObservabilityMdcInitializer(Tracer tracer) {
            this.tracer = tracer;
        }

        /**
         * 将当前 Span 的 traceId 和 spanId 写入 MDC。
         * 通常在过滤器或拦截器中调用。
         */
        public void populateMdc() {
            if (tracer.currentSpan() != null) {
                var spanContext = tracer.currentSpan().context();
                MDC.put(TRACE_ID_KEY, spanContext.traceId());
                MDC.put(SPAN_ID_KEY, spanContext.spanId());
            }
        }

        /**
         * 清除 MDC 中的 traceId 和 spanId。
         */
        public void clearMdc() {
            MDC.remove(TRACE_ID_KEY);
            MDC.remove(SPAN_ID_KEY);
        }
    }
}
