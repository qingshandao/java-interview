package com.gc.multithreaddemo.demos.reentrantLock;

import org.apache.tomcat.jni.Time;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class DeadlockAvoidWithTryLock {

    static ReentrantLock lockA = new ReentrantLock();
    static ReentrantLock lockB = new ReentrantLock();
    public static void main(String[] args) {
        // 线程1
        new Thread(() -> {
            while (true){
                boolean gotA = false;
                boolean gotB = false;
                try {
                    gotA = lockA.tryLock(1, TimeUnit.SECONDS);
                    if (gotA){
                        System.out.println("线程1获取 lockA");
                        sleep(1000);
                        gotB = lockB.tryLock(1, TimeUnit.SECONDS);
                        if(gotB){
                            System.out.println("线程1获取 lockB");
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    return;
                }finally {
                    if (gotB) lockB.unlock();
                    if (gotA) lockA.unlock();
                }
            }
        }, "thread-1").start();

        // 线程2
        new Thread(() -> {
            while (true){
                boolean gotA = false;
                boolean gotB = false;

                try {
                    gotB = lockB.tryLock(1, TimeUnit.SECONDS);
                    if(gotB){
                        System.out.println("线程2 获取 lockB");
                        sleep(2000);
                        gotA = lockA.tryLock(1, TimeUnit.SECONDS);
                        if(gotA){
                            System.out.println("线程2 获取 lockA");
                            break;
                        }
                    }
                }catch (InterruptedException e){
                    return;
                }finally {
                    if(gotA) lockA.unlock();
                    if(gotB) lockB.unlock();
                }
            }
        }, "thread-2").start();
    }


    static void sleep(long ms){
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
