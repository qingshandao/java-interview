package com.gc.multithreaddemo.demos.createThread.thread;

public class MyThread extends Thread{

    @Override
    public void run() {
        System.out.println("线程执行：" + Thread.currentThread().getName());
    }
}
