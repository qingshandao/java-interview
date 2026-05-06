package com.gc.multithreaddemo.demos.aqs;

import java.util.concurrent.CyclicBarrier;

/**
 * CyclicBarrier 演示2：体现“Cyclic（可复用）
 */
public class CyclicBarrierDemo2 {
    public static void main(String[] args) {
        CyclicBarrier cyclicBarrier = new CyclicBarrier(2, () -> {
            System.out.println("\n>>> 两个线程已汇合，进入下一轮\n");
        });

        Runnable task = () -> {
            try {
                for (int i = 0; i < 2; i++) {
                    int round = i;
                    System.out.println(Thread.currentThread().getName() + "第 " + (round + 1) + " 轮开始");

                    Thread.sleep(1000);

                    System.out.println(Thread.currentThread().getName() + "到达屏障");

                    cyclicBarrier.await();

                    System.out.println(Thread.currentThread().getName() + "第 " + (round + 1) + " 轮结束");
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        };

        new Thread(task, "线程A").start();
        new Thread(task, "线程B").start();
    }
}
