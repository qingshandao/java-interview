package com.gc.multithreaddemo.demos.threadPool;

import org.apache.tomcat.jni.Time;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * PriorityBlockingQueue 定义顺序的方案2：创建队列时指定排序方式
 */
public class PriorityDemo2 {
    public static void main(String[] args) {
        // 1.定义优先级队列
        PriorityBlockingQueue<Runnable> queue = new PriorityBlockingQueue<>(11, (r1, r2) -> {
            PriorityTask2 task1 = (PriorityTask2) r1;
            PriorityTask2 task2 = (PriorityTask2) r2;
            return Integer.compare(task1.getPriority(), task2.getPriority());
        });

        // 2.创建线程池
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                1, 1,
                60, TimeUnit.SECONDS,
                queue);

        // 3.创建任务
        threadPoolExecutor.execute(new PriorityTask2("threa-1", 1));
        threadPoolExecutor.execute(new PriorityTask2("threa-2", 3));
        threadPoolExecutor.execute(new PriorityTask2("threa-3", 2));

        // 4.关闭线程池
        threadPoolExecutor.shutdown();
    }
}

class PriorityTask2 implements Runnable{

    private String name;
    private int priority;

    public PriorityTask2(String name, int priority) {
        this.name = name;
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public void run() {
        System.out.println("执行：" + name + "，优先级：" + priority +
                "，线程：" + Thread.currentThread().getName());
    }
}
