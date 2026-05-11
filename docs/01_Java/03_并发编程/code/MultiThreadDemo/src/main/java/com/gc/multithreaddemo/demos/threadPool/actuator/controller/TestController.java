package com.gc.multithreaddemo.demos.threadPool.actuator.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 模拟线程池任务
 */
@RestController
public class TestController {

    @Resource(name = "orderExecutor")
    private ThreadPoolExecutor executor;

    @GetMapping("/test")
    public String test() {

        for (int i = 0; i < 20; i++) {

            executor.execute(() -> {

                try {

                    System.out.println(
                            Thread.currentThread().getName()
                    );

                    Thread.sleep(10000);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            });
        }

        return "ok";
    }
}