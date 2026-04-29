package com.gc.multithreaddemo.demos.threadPool;

import java.util.concurrent.*;

public class threadPoolRejectDemo {
    public static void main(String[] args) throws InterruptedException {

        testPolicy("AbortPolicy", new ThreadPoolExecutor.AbortPolicy());
        testPolicy("CallerRunsPolicy", new ThreadPoolExecutor.CallerRunsPolicy());
        testPolicy("DiscardPolicy", new ThreadPoolExecutor.DiscardPolicy());
        testPolicy("DiscardOldestPolicy", new ThreadPoolExecutor.DiscardOldestPolicy());

    }

    private static void testPolicy(String policyName, RejectedExecutionHandler handler) throws InterruptedException{

        System.out.println("\n===============================");
        System.out.println("测试策略：" + policyName);
        System.out.println("=================================");

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                2,      // 2个核心线程
                2,      // 2个线程池最大线程数
                10,     // 空余线程等待时间
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(2),    // 任务队列长度为2
                Executors.defaultThreadFactory(),
                handler
        );

        // 提交6个任务（大于最大线程数 + 任务队列数，必定触发拒绝策略）

        for (int i = 1; i <= 6; i++) {
            final int taskId = i;

            try {
                threadPoolExecutor.execute(() -> {
                    String threadName = Thread.currentThread().getName();
                    System.out.println("任务 " + taskId + " 执行线程：" + threadName);

                    // 模拟任务耗时
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    System.out.println("任务 " + taskId + " 执行成功！");
                });
            }catch (Exception e){
                System.out.println("任务 " + taskId + " 被拒绝：" + e);
            }
        }

        threadPoolExecutor.shutdown();
        threadPoolExecutor.awaitTermination(10, TimeUnit.SECONDS);
    }
}
