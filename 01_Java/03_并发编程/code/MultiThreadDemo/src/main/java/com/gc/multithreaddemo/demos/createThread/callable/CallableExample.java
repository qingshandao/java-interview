package com.gc.multithreaddemo.demos.createThread.callable;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class CallableExample {
    public static void main(String[] args) throws Exception {
        System.out.println("主线程开始时间：" + System.currentTimeMillis());

        // 1.创建 Callable 实例
        MyCallable callableTask1 = new MyCallable("任务1");
        MyCallable callableTask2 = new MyCallable("任务2");

        // 2.将Callable包装到FutureTask中
        FutureTask<String> futureTask1 = new FutureTask<>(callableTask1);
        FutureTask<String> futureTask2 = new FutureTask<>(callableTask2);

        System.out.println("FutureTask创建完成，此时call()方法还未执行");

        // 3.创建线程并启动
        Thread thread1 = new Thread(futureTask1);
        Thread thread2 = new Thread(futureTask2);

        System.out.println("开始启动线程...");
        long startTime = System.currentTimeMillis();
        thread1.start(); // 此时线程开始，但call()方法的执行取决于FutureTask的run()方法调用
        thread2.start();

        System.out.println("线程已启动，但此时call()方法仍在等待FutureTask.run()被调用");
        System.out.println("现在开始获取结果...");

        // 4.获取结果 - 这里会阻塞直到call()方法执行完成
        String result1 = futureTask1.get();
        String result2 = futureTask2.get();

        long endTime = System.currentTimeMillis();

        System.out.println("结果1：" + result1);
        System.out.println("结果2：" + result2);
        System.out.println("总耗时：" + (endTime - startTime) + "ms");


        // 5，异常情况演示
        Callable<String> errorCallableTask = () -> {
            System.out.println("异常任务开始执行");
            Thread.sleep(500);
            throw new RuntimeException("异常任务执行, 抛出异常");
        };

        FutureTask<String> errorFutureTask = new FutureTask<>(errorCallableTask);
        Thread errorThread = new Thread(errorFutureTask);
        errorThread.start();

        try {
            String errorResult = errorFutureTask.get();
            System.out.println("异常结果：" + errorResult);
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }
}
