package com.gc.multithreaddemo.demos.createThread.runtThread;

public class MainThread {
    public static void main(String[] args) throws InterruptedException {
        CountingThread thread = new CountingThread();

        System.out.println("=== 主线程中多次调用run()方法 ===");
        System.out.println("主线程：" + Thread.currentThread().getName());

        // 第一次调用run()方法，在主线程中执行
        System.out.println("\n第一次调用run()：");
        long start1 = System.currentTimeMillis();
        thread.run();
        long end1 = System.currentTimeMillis();
        System.out.println("第一次run()执行耗时：" + (end1 - start1) + "ms");

        // 第二次调用run()方法，在主线程中执行，仍在主线程中
        System.out.println("\n第一次调用run()：");
        long start2 = System.currentTimeMillis();
        thread.run();
        long end2 = System.currentTimeMillis();
        System.out.println("第一次run()执行耗时：" + (end2 - start2) + "ms");

        // 第三次调用run() - 仍在主线程中执行
        System.out.println("\n第三次调用run()：");
        long start3 = System.currentTimeMillis();
        thread.run();
        long end3 = System.currentTimeMillis();
        System.out.println("第三次run()执行耗时：" + (end3 - start3) + "ms");

        System.out.println("\n=== 对比：使用start()方法启动线程 ===");
        // 创建新的线程对象测试
        CountingThread threadForStart = new CountingThread();
        System.out.println("\n调用start()方法：");
        long start4 = System.currentTimeMillis();
        threadForStart.start();
        long end4 = System.currentTimeMillis();
        // 注意：这里不会等待线程完成，立即继续执行
        System.out.println("start()调用后立即继续，时间差：" +
                (System.currentTimeMillis() - start4) + "ms");

        // 等待线程完成
        threadForStart.join();

        System.out.println("\n=== 总结 ===");
        System.out.println("1. run()方法可以被多次调用（就像普通方法）");
        System.out.println("2. 每次调用run()都在当前线程中同步执行");
        System.out.println("3. start()只能调用一次，且会创建新线程");
        System.out.println("4. 直接调用run()无法实现多线程并发");

        // 展示线程状态变化
        Thread stateDemo = new Thread(() -> {
            System.out.println("线程正在运行");
        });

        System.out.println("新建线程状态：" + stateDemo.getState());
        stateDemo.start();
        System.out.println("启动后线程状态：" + stateDemo.getState());
        stateDemo.join();
        System.out.println("完成后线程状态：" + stateDemo.getState());

    }
}
