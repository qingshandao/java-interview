package com.gc.multithreaddemo.demos.threadPool;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 预热线程池中的核心线程
 */
public class threadPoolPreStart {
    public static void main(String[] args) throws InterruptedException {
        // 1.不预热线程池
        System.out.println("\n==================");
        System.out.println("不预热线程池");
        System.out.println("==================");

        ThreadPoolExecutor threadPoolExecutor1 = new ThreadPoolExecutor(
                3,
                3,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>()
        );

        System.out.println("提交任务前，线程池中的线程数：" + threadPoolExecutor1.getPoolSize());
        threadPoolExecutor1.execute(() -> {
            System.out.println("任务1执行的线程为：" + Thread.currentThread().getName());
        });
        Thread.sleep(500);  // 等待任务提交完毕，核心线程创建完毕
        System.out.println("提交任务后，线程池中的线程数：" + threadPoolExecutor1.getPoolSize());

        // 2.预热所有核心线程
        System.out.println("\n==================");
        System.out.println("预热所有核心线程池");
        System.out.println("==================");
        ThreadPoolExecutor threadPoolExecutor2 = new ThreadPoolExecutor(
                3,
                3,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>()
        );
        threadPoolExecutor2.prestartAllCoreThreads();
        System.out.println("提交任务前，线程池中的线程数：" + threadPoolExecutor2.getPoolSize());
        threadPoolExecutor2.execute(() -> {
            System.out.println("任务2执行的线程为：" + Thread.currentThread().getName());
        });
        System.out.println("提交任务后，线程池中的线程数：" + threadPoolExecutor2.getPoolSize());

        // 3.预热一个核心线程
        System.out.println("\n==================\n预热1个核心线程池\n==================");
        ThreadPoolExecutor threadPoolExecutor3 = new ThreadPoolExecutor(
                2,
                2,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>()
        );
        // 3.1、预热第一个核心线程
        boolean prestarted = threadPoolExecutor3.prestartCoreThread();
        System.out.println("提交任务3.1前，线程池中的线程数：" + threadPoolExecutor3.getPoolSize() + "，预热结果为：" + prestarted);

        // 3.2、execute() 也会创建线程
        threadPoolExecutor3.execute(() -> {
            System.out.println("任务3.1执行的线程为：" + Thread.currentThread().getName());
        });
        System.out.println("提交任务3.1后，线程池中的线程数：" + threadPoolExecutor3.getPoolSize());

        // 3.3、此时核心线程数已经达到2个，继续创建核心线程失败
        prestarted = threadPoolExecutor3.prestartCoreThread();
        System.out.println("提交任务3.2前，线程池中的线程数：" + threadPoolExecutor3.getPoolSize() + "，预热结果为：" + prestarted);
        threadPoolExecutor3.execute(() -> {
            System.out.println("任务3.2执行的线程为：" + Thread.currentThread().getName());
        });
        System.out.println("提交任务3.2后，线程池中的线程数：" + threadPoolExecutor3.getPoolSize());


        // 4.关闭线程池
        threadPoolExecutor1.shutdown();
        threadPoolExecutor2.shutdown();
        threadPoolExecutor3.shutdown();

    }
}
