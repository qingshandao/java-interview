package com.gc.multithreaddemo.demos.createThread.callable;

import java.util.concurrent.Callable;

public class MyCallable implements Callable<String> {

    private String taskName;

    public MyCallable(String taskName){
        this.taskName = taskName;
    }

    @Override
    public String call() throws Exception {
        System.out.println(this.taskName + " 开始执行， 线程名称：" + Thread.currentThread().getName());
        Thread.sleep(1000);
        System.out.println(this.taskName + " 执行结束，线程名称：" + Thread.currentThread().getName());
        return "任务" + this.taskName + " 执行完成，线程：" + Thread.currentThread().getName();
    }
}
