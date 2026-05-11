package com.gc.multithreaddemo.demos.threadPool.actuator.config;

import com.gc.multithreaddemo.demos.threadPool.actuator.factory.NamedThreadFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolConfig {

    @Bean("orderExecutor")
    public ThreadPoolExecutor orderExecutor() {

        return new ThreadPoolExecutor(
                2,
                4,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(10),
                new NamedThreadFactory("order-thread"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}