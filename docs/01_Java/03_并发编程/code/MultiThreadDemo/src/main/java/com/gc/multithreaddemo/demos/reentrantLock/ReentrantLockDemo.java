package com.gc.multithreaddemo.demos.reentrantLock;

import org.apache.ibatis.javassist.compiler.ast.Variable;

import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockDemo {

    private static ReentrantLock reentrantLock = new ReentrantLock();
    public static void main(String[] args) throws InterruptedException {

        Thread t1 = new Thread(() -> {
            try {
                reentrantLock.lockInterruptibly();
                // reentrantLock.lock();
                System.out.println("t1 获取到锁");

                // 模拟线程占用
                Thread.sleep(1000 * 10);

                System.out.println("t1 结束任务");
            }catch (InterruptedException e){
                System.out.println("t1 被中断");
            }finally {
                if (reentrantLock.isHeldByCurrentThread()){
                    reentrantLock.unlock();
                }
            }
        });

        Thread t2 = new Thread(() -> {
            try{
                Thread.sleep(1000);     // 确保t1获取到锁

                System.out.println("t2 尝试获取锁");
                reentrantLock.lockInterruptibly();
                // reentrantLock.lock();
                System.out.println("t2 获取到锁");
            }catch (InterruptedException e){
                System.out.println("t2 在等待时被中断");
            }finally {
                if(reentrantLock.isHeldByCurrentThread()){
                    reentrantLock.unlock();
                }
            }
        });

        t1.start();
        t2.start();

        // 主线程等待一会儿，中断t2的等待
        Thread.sleep(1000);
        System.out.println("主线程中断 t1");
        t1.interrupt();

    }
}
