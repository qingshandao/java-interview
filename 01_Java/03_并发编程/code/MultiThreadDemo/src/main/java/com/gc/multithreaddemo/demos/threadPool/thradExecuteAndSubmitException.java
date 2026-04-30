package com.gc.multithreaddemo.demos.threadPool;

import java.util.concurrent.*;

/**
 * 线程池提交任务的两种方式区别
 * - execute()：异常会“杀死”线程
 * - submit()：异常被“吃掉”，可以复用线程
 */
public class thradExecuteAndSubmitException {
    public static void main(String[] args) throws Exception{
        System.out.println("\n============== execute()提交任务 ==============");
        // 线程池仅提供1个线程，方便观察抛出异常后的线程处理方式
        ThreadPoolExecutor executeExecutor = new ThreadPoolExecutor(
                1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>()
        );

        executeExecutor.execute(() -> {
            System.out.println("任务1线程：" + Thread.currentThread().getName());
            throw new RuntimeException("任务1异常");
        });

        Thread.sleep(1000);
        executeExecutor.execute(() -> {
            System.out.println("任务2线程：" + Thread.currentThread().getName());
        });

        Thread.sleep(1000);
        executeExecutor.shutdown();

        System.out.println("\n============== submit()提交任务 ==============");
        // 线程池仅提供1个线程，方便观察抛出异常后的线程处理方式
        ThreadPoolExecutor submitExecutor = new ThreadPoolExecutor(
                1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>()
        );

        Future future = submitExecutor.submit(() -> {
            System.out.println("任务1线程：" + Thread.currentThread().getName());
            throw new RuntimeException("任务1异常");
        });
        Thread.sleep(1000);
        submitExecutor.submit(() -> {
            System.out.println("任务2线程：" + Thread.currentThread().getName());
        });
        try {
            future.get();
        }catch (ExecutionException e){
            System.out.println("捕获异常：" + e.getCause());
        }
        Thread.sleep(1000);
        submitExecutor.shutdown();
    }
}
