package com.gc.multithreaddemo.demos.synchronizedDemo;

public class ObjectSyncDemo {
    public synchronized void instanceMethod(){
        System.out.println(Thread.currentThread().getName() + " -> instanceMehtod start");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(Thread.currentThread().getName() + " -> instanceMethod end");
    }

    public static void main(String[] args) {
        ObjectSyncDemo obj1 = new ObjectSyncDemo();
        ObjectSyncDemo obj2 = new ObjectSyncDemo();

        // 两个线程访问同一个对象 -> 互斥
        new Thread(() -> obj1.instanceMethod(), "T1").start();
        new Thread(() -> obj1.instanceMethod(), "T2").start();

        // 两个线程访问不同对象 -> 不互斥
        new Thread(() -> obj2.instanceMethod(), "T3").start();

    }

}
