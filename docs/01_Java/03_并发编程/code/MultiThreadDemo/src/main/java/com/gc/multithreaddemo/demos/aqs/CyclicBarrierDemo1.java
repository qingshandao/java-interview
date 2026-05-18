package com.gc.multithreaddemo.demos.aqs;

import java.util.concurrent.CyclicBarrier;

/**
 * CyclicBarrier 演示1：所有线程到齐后再继续
 */
public class CyclicBarrierDemo1 {
    public static void main(String[] args) {
        int barrierNum = 3;

        // 1.定义CyclicBarrier的等待线程数
        CyclicBarrier cyclicBarrier = new CyclicBarrier(3, () -> {
            System.out.println("\n>>> 所有线程已到达屏障，开始统一执行下一阶段 ");
        });

        // 2.多线程示例
        for (int i = 0; i < 30; i++) {
            int id = i;
            new Thread(() -> {
                try {
                    System.out.println("线程 " + (id + 1) + " 执行第一阶段任务");
                    Thread.sleep(1000 * id);

                    System.out.println("线程 " + (id + 1) + " 抵达屏障，开始等待其它线程");

                    cyclicBarrier.await();

                    System.out.println("线程 " + (id + 1) + " 开始执行第二阶段任务");
                }catch (Exception e){
                    e.printStackTrace();
                }
            }).start();
        }

    }
}
