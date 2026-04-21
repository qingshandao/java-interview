package com.gc.multithreaddemo.demos.synchronizedDemo;

public class ClassSyncDemo {
    public static synchronized void staticMehtod(){
        System.out.println(Thread.currentThread().getName() + " -> staticMethod start");
        try {
            Thread.sleep(2000);
        }catch (InterruptedException e){
            System.out.println(e.getMessage());
        }
        System.out.println(Thread.currentThread().getName() + " -> staticMethod end");
    }

    public static void main(String[] args) {
        ClassSyncDemo obj1 = new ClassSyncDemo();
        ClassSyncDemo obj2 = new ClassSyncDemo();

        // 不同对象调用，但是锁同一个
        new Thread(() -> ClassSyncDemo.staticMehtod(), "T1").start();
        new Thread(ClassSyncDemo::staticMehtod, "T2").start();
        new Thread(ClassSyncDemo::staticMehtod, "T3").start();
    }
}
