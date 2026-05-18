package com.gc.multithreaddemo.demos.aqs;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CountDownLatchExample2 {

    private static final int THREAD_COUNT = 5;

    public static void main(String[] args) throws InterruptedException {

        // 发令枪（初始值为1）
        CountDownLatch latch = new CountDownLatch(1);

        // 运动员数量
        ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadNum = i;
            threadPool.execute(() -> {
                System.out.println("运动员-" + threadNum + " 已经做好准备");
                try {
                    // 等待发令枪
                    latch.await();

                    System.out.println("运动员-" + threadNum + " 开始跑步");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

        Thread.sleep(5000);
        System.out.println("裁判准备完毕！");
        Thread.sleep(1000);
        System.out.println("裁判宣布：比赛开始！");
        latch.countDown();

        threadPool.shutdown();

    }
}
