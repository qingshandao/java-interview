package com.gc.multithreaddemo.demos.reentrantLock;

import java.util.concurrent.locks.ReentrantLock;

public class FairLockDemo {

    // 默认为false非公平锁
    private static ReentrantLock lock = new ReentrantLock(true);

    public static void main(String[] args) {
        for (int i = 0; i < 5; i++) {
            Thread thread = new Thread(new Task(), "线程-" + i);
            thread.start();
        }
    }

    static class Task implements Runnable{
        @Override
        public void run() {
            System.out.println(Thread.currentThread().getName() + " 尝试获取锁");

            lock.lock();
            try {
                System.out.println(Thread.currentThread().getName() + " 获取到锁");
                try {
                    // 模拟执行时间
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }finally {
                lock.unlock();
            }
        }
    }
}
