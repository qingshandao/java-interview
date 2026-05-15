package com.gc.multithreaddemo.demos.synchronizedDemo;

public class WaitNotifyDemo {
    private static final Object LOCK = new Object();

    public static void main(String[] args) {
        // 1.消费线程
        Thread consumer1 = new Thread(() -> {
            synchronized (LOCK) {
                System.out.println("消费者线程-1：准备等待");

                try {
                    // 调用wait()，当前线程进入 WAITTING 状态
                    // 释放LOCK锁
                    LOCK.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                System.out.println("消费者线程-1：继续执行");
            }
        });
        // 1.消费线程
        Thread consumer2 = new Thread(() -> {
            synchronized (LOCK) {
                System.out.println("消费者线程-2：准备等待");

                try {
                    // 调用wait()，当前线程进入 WAITTING 状态
                    // 释放LOCK锁
                    LOCK.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                System.out.println("消费者线程-2：继续执行");
            }
        });

        // 2.生产者线程
        Thread producer = new Thread(() -> {

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            synchronized (LOCK){
                System.out.println("生产者发出通知");
                // 唤醒一个等待线程
                LOCK.notify();

                // 唤醒所有线程
                // LOCK.notifyAll();
            }
        });

        consumer1.start();
        consumer2.start();
        producer.start();

    }

}
