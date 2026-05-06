package com.gc.multithreaddemo.demos.threadPool;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * PriorityBlockingQueue 定义顺序的方案1：在任务对象中实现 Comparable接口，重写 compareTo 方法
 */
public class PriorityDemo1 {
    public static void main(String[] args) {
        // 1.创建线程池，任务队列为 PriorityBlockingQueue
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                1, 1,
                6, TimeUnit.SECONDS,
                new PriorityBlockingQueue<>()
        );

        // 2.创建实现了Comparable接口的任务
        threadPoolExecutor.execute(new PriorityTask1("task-1", 1));   // 最高优先级
        threadPoolExecutor.execute(new PriorityTask1("task-2", 3));   // 最低优先级
        threadPoolExecutor.execute(new PriorityTask1("task-3", 2));   // 中等优先级

        // 关闭线程池
        threadPoolExecutor.shutdown();
    }
}


class PriorityTask1 implements Runnable, Comparable<PriorityTask1>{

    private String name;
    private int priority;

    public PriorityTask1(String name, int priority){
        this.name = name;
        this.priority = priority;       // 数值越小，优先级越高
    }

    @Override
    public int compareTo(PriorityTask1 o) {
        // 小的优先级先执行
        return Integer.compare(this.priority, o.priority);
    }

    @Override
    public void run() {
        System.out.println("执行：" + name + "，优先级：" + priority +
                "，线程：" + Thread.currentThread().getName());
    }
}
