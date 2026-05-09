package com.gc.multithreaddemo.demos.createThread.threadPool;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ThreadPoolExample {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(3);

        for (int i = 0; i < 5; i++) {
            final int taskId = i;

            Future<?> future = fixedThreadPool.submit(() -> {
                System.out.println("任务：" + taskId + " 执行中， 线程：" + Thread.currentThread().getName());

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });

            System.out.println(Thread.currentThread().getName() + " 执行结果：" + future.get());

        }

        fixedThreadPool.shutdown();
    }
}
