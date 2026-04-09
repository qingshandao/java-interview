package com.gc.multithreaddemo.demos.contextSwitch;

public class ContextSwitchDemo {
    private static int counter = 0;

    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                System.out.println(Thread.currentThread().getName() + " 执行，counter=" + counter++);
                try {
                    Thread.sleep(10); // 主动让出 CPU，触发上下文切换
                } catch (InterruptedException e) { e.printStackTrace(); }
            }
        }, "Thread-1");

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                System.out.println(Thread.currentThread().getName() + " 执行，counter=" + counter++);
                try {
                    Thread.sleep(10); // 同样触发上下文切换
                } catch (InterruptedException e) { e.printStackTrace(); }
            }
        }, "Thread-2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }
}
