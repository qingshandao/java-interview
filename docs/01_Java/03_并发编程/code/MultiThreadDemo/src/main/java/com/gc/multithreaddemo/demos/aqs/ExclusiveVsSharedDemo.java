package com.gc.multithreaddemo.demos.aqs;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ExclusiveVsSharedDemo {
    public static void main(String[] args) throws InterruptedException {
        // 1.ReentrantLock 独占锁演示示例
        // reentrantLockDemo();

        // 2.Semaphore 共享模式
        // Thread.sleep(10000);
        // semaphoreDemo();

        /*
         * 3.CountDownLatch
         * 共享模式
         */
        // Thread.sleep(10000);
        // countDownLatchDemo();

        /*
         * 4.ReentrantReadWriteLock
         * 读共享 + 写独占
         */
        readWriteLockDemo();


    }

    /**
     * ReentrantLock
     * 独占模式
     */
    public static void reentrantLockDemo(){
        ReentrantLock lock = new ReentrantLock();

        Runnable task = () -> {
            lock.lock();
            try {
                System.out.println(Thread.currentThread().getName() + " 拿到了ReentrantLock，获取独立锁成功");
                Thread.sleep(1000);
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                System.out.println(Thread.currentThread().getName() + " 释放了ReentrantLock");
                lock.unlock();
            }
        };

        System.out.println("\n ======= ReentrantLock（独占锁模式）=========");
        for (int i = 0; i < 5; i++) {
            new Thread(task, "独占锁线程-" + i).start();
        }
    }

    /**
     * Semaphore
     * 共享模式
     */
    public static void semaphoreDemo(){
        // 同时允许3个线程获取许可证
        Semaphore semaphore = new Semaphore(3);

        Runnable task = () -> {
            try {
                semaphore.acquire();
                System.out.println(Thread.currentThread().getName() + " 获取到了Semaphore共享资源");
                Thread.sleep(1000);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }finally {
                System.out.println(Thread.currentThread().getName() + " 释放了Semaphore共享资源");
                semaphore.release();
            }
        };

        System.out.println("\n ======= Semaphore共享资源（共享模式）=========");
        for (int i = 0; i < 5; i++) {
            new Thread(task, "Semaphore共享线程-" + i).start();
        }
    }

    /*
     * 3.CountDownLatch
     * 共享模式
     */
    public static void countDownLatchDemo(){
        // 需要执行的任务数
        int countNum = 3;
        // state = 3
        CountDownLatch latch = new CountDownLatch(countNum);

        Runnable task = () -> {
            try {
                System.out.println(Thread.currentThread().getName() + " 开始执行任务");
                Thread.sleep(1000);
                System.out.println(Thread.currentThread().getName() + " 执行完毕任务");
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                latch.countDown();
                System.out.println("剩余的执行次数：" + latch.getCount());
            }
        };

        System.out.println("\n========== CountDownLatch（共享模式） ==========");
        for (int i = 0; i < countNum; i++) {
            new Thread(task, "CountDownLatch共享线程-" + i).start();
        }

        System.out.println("\n ================ 主线程等待所有的任务执行完毕 =====================");
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("\n ================ 主线程等待所有的任务执行完毕 =====================");

    }

    /**
     * ReentrantReadWriteLock
     * 读共享 + 写独占
     */
    public static void readWriteLockDemo(){
        ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
        ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

        // 读任务
        Runnable readTask = () -> {
            readLock.lock();

            try {
                System.out.println(Thread.currentThread().getName() + " 获取到读锁");
                Thread.sleep(1000);
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                System.out.println(Thread.currentThread().getName() + " 释放读锁");
                readLock.unlock();
            }
        };

        // 写任务
        Runnable writeTask = () -> {
            writeLock.lock();

            try {
                System.out.println(Thread.currentThread().getName() + " 获取到写锁");
                Thread.sleep(1000);
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                System.out.println(Thread.currentThread().getName() + " 释放写锁");
                writeLock.unlock();
            }
        };

        System.out.println("\n========== ReentrantReadWriteLock ==========");

        // 读线程可以同时进行
        for (int i = 0; i < 3; i++) {
            new Thread(readTask, "读线程-" + 1).start();
        }

        // 写线程必须单个进行
        for (int i = 0; i < 3; i++) {
            new Thread(writeTask, "写线程-" + i).start();
        }
    }
}
