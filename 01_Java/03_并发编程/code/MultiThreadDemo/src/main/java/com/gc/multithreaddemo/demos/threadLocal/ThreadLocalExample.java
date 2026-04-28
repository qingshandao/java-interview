package com.gc.multithreaddemo.demos.threadLocal;

public class ThreadLocalExample {
    private static final ThreadLocal<Integer> THREAD_LOCAL = ThreadLocal.withInitial(() -> 0);

    public static void main(String[] args) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                THREAD_LOCAL.set(10);
                System.out.println(Thread.currentThread().getName() + " 线程中的 THREAD_LOCAL 值为：" + THREAD_LOCAL.get());
            }
        };

        Thread thread1 = new Thread(task, "thread-1");
        Thread thread2 = new Thread(task, "thread-1");

        thread1.start();
        thread2.start();

    }
}
