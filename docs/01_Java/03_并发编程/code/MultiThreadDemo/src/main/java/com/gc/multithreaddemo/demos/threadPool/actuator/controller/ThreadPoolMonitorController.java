package com.gc.multithreaddemo.demos.threadPool.actuator.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * ThreadPoolExecutor 自己监控线程池状态
 * 如果启用 actuator 监控，直接访问：http://localhost:8080/actuator/metrics，这是 actuator 提供的接口
 */
@RestController
@RequestMapping("/threadPool")
public class ThreadPoolMonitorController {

    @Resource(name = "orderExecutor")
    private ThreadPoolExecutor executor;

    @GetMapping("/info")
    public Map<String, Object> info() {

        Map<String, Object> result = new HashMap<>();

        // 核心线程数
        result.put("corePoolSize",
                executor.getCorePoolSize());

        // 最大线程数
        result.put("maximumPoolSize",
                executor.getMaximumPoolSize());

        // 当前线程数
        result.put("poolSize",
                executor.getPoolSize());

        // 活跃线程数
        result.put("activeCount",
                executor.getActiveCount());

        // 已完成任务数
        result.put("completedTaskCount",
                executor.getCompletedTaskCount());

        // 队列任务数
        result.put("queueSize",
                executor.getQueue().size());

        // 队列剩余容量
        result.put("remainingCapacity",
                executor.getQueue().remainingCapacity());

        return result;
    }
}
