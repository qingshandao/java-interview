package com.gc.multithreaddemo.demos.aqs;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class SemaphoreExample {
    // 请求的数量
    private static final int THREAD_COUNT = 550;

    public static void main(String[] args) {
        ExecutorService threadPool = Executors.newFixedThreadPool(300);

        // 许可证的数量
        Semaphore semaphore = new Semaphore(20);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadCount = i;
            threadPool.execute(() -> {
                try {
                    semaphore.acquire();
                    System.out.println("------ threadNum: " + threadCount + " ------");
                    Thread.sleep(1000);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {
                    semaphore.release();
                }
            });
        }

        threadPool.shutdown();
        System.out.println("finishd");
    }
}
