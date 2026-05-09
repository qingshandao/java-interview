package com.gc.multithreaddemo.demos.createThread.runtThread;

public class CountingThread extends Thread{

    private static int executionCount = 0;

    @Override
    public void run() {
        executionCount++;

        System.out.println("第" + executionCount + "次执行run()方法，" +
                Thread.currentThread().getName()+ " 线程的执行时间为："+
                System.currentTimeMillis());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            System.out.println("线程被中断");
        }

        System.out.println("第" + executionCount + "次线程执行run()方法结束");
    }

    public static void main(String[] args) {

    }
}
