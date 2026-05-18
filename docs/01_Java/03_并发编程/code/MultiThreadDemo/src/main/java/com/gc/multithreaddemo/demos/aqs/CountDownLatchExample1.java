package com.gc.multithreaddemo.demos.aqs;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CountDownLatchExample1 {

    // 请求的数量
    private static final int THREAD_COUNT = 300;

    public static void main(String[] args) throws InterruptedException {
        // 创建一个具有固定线程数量的线程池对象（如果这里线程池的线程数量给太少的话你会发现执行的很慢）
        // 只是测试使用，实际场景请手动赋值线程池参数
        ExecutorService threadPool = Executors.newFixedThreadPool(550);

        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT - 1; i++) {
            final int threadNum = i;
            threadPool.execute(() -> {
                try {
                    test(threadNum);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {
                    // 一个请求结束
                    latch.countDown();
                }
            });
        }

        latch.await();

        threadPool.shutdown();

        System.out.println("finished");

    }

    public static void test(int threadNum) throws InterruptedException {
        Thread.sleep(1000);
        System.out.println("threadNum = " + threadNum);
        Thread.sleep(1000);
    }
}
