package com.gc.multithreaddemo.demos.createThread.runnable;

public class MyRunnable implements Runnable{
    @Override
    public void run() {
        System.out.println("Runnable线程执行： " + Thread.currentThread().getName());
    }
}
