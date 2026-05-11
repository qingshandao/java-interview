package com.gc.multithreaddemo.demos.threadPool.actuator.config;

/**
 * 注册线程池指标
 */

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadPoolMetricsConfig {

    private final MeterRegistry meterRegistry;

    private final ThreadPoolExecutor executor;

    public ThreadPoolMetricsConfig(
            MeterRegistry meterRegistry,
            @Qualifier("orderExecutor")
            ThreadPoolExecutor executor) {

        this.meterRegistry = meterRegistry;
        this.executor = executor;
    }

    @PostConstruct
    public void bindMetrics() {

        // 注册线程池监控
        ExecutorServiceMetrics.monitor(
                meterRegistry,
                executor,
                "orderExecutor"
        );
    }
}
