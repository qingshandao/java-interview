package com.gc.multithreaddemo.demos.createThread.threadPool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPoolExample {
    public static void main(String[] args) {
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(3);

        for (int i = 0; i < 5; i++) {
            final int taskId = i;

            fixedThreadPool.submit(() -> {
                System.out.println("任务：" + taskId + " 执行中， 线程：" + Thread.currentThread().getName());

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });

        }

        fixedThreadPool.shutdown();
    }
}
