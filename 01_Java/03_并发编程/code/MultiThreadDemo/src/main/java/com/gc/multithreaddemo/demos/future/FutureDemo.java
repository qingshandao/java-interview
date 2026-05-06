package com.gc.multithreaddemo.demos.future;

import java.util.concurrent.*;

/**
 * Future 功能演示demo
 */
public class FutureDemo {
    public static void main(String[] args) throws InterruptedException {
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(2);

        // 1.提交任务
        Future<Integer> future = fixedThreadPool.submit(() -> {
            System.out.println("子任务开始执行: " + Thread.currentThread().getName());

            for (int i = 0; i < 5; i++) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("任务执行中。。。" + i);
            }

            return 100;
        });

        // 2.主线程做其他事情
        for (int i = 0; i < 5; i++) {
            Thread.sleep(1000);
            System.out.println("主线程做其他事情");
        }

        // 3.查询子线程任务状态
        System.out.println("任务是否完成：" + future.isDone());
        System.out.println("任务是否被取消：" + future.isCancelled());

        // 4.尝试需要任务（可选）
        // boolean cancelled = future.cancel(true);
        // System.out.println("是否成功取消：" + cancelled);

        // 5.获取执行结果
        try {
            Integer result = future.get();
            System.out.println("任务执行结果：" + result);
        } catch (CancellationException cancellationException){
            System.out.println("任务被取消");
        } catch (ExecutionException e) {
            System.out.println("任务执行异常");
        }

        fixedThreadPool.shutdown();
    }
}
