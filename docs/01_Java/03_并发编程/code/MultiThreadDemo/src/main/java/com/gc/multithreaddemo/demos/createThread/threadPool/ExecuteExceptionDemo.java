package com.gc.multithreaddemo.demos.createThread.threadPool;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ExecuteExceptionDemo {
    public static void main(String[] args) {
        ThreadFactory factory = r -> {
            Thread thread = new Thread(r);

            // 设置异常处理器
            thread.setUncaughtExceptionHandler(
                    (t, e) -> {
                        System.out.println("线程：" + t.getName());
                        System.out.println("捕获异常：" + e.getCause());
                    }
            );
            return thread;
        };

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), factory);

        threadPoolExecutor.execute(() -> {
            System.out.println("任务开始");

            int x = 1/0;
        });

        threadPoolExecutor.shutdown();
    }
}
