package com.gc.multithreaddemo.demos.threadLocal;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.alibaba.ttl.threadpool.TtlExecutors;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class threadLocalContext {
    private static final ThreadLocal<String> USER_TL = new ThreadLocal<>();

    private static final InheritableThreadLocal<String> USER_TL_INHERI = new InheritableThreadLocal<>();

    private ExecutorService pool = Executors.newFixedThreadPool(1);

    // 使用 TransmittableThreadLocal 解决线程隔离问题
    private static final TransmittableThreadLocal<String> USER_TL_Transmit = new TransmittableThreadLocal<>();
    private ExecutorService poolWithTtl = TtlExecutors.getTtlExecutorService(
            Executors.newFixedThreadPool(1)
    );

    /**
     * 由于Thread的线程隔离，无法获取主线程的ThreadLocal值
     */
    public void handleRequestWithError(){
        USER_TL.set("userName");

        pool.submit(() -> {
            doBusiness();
        });

        USER_TL.remove();

        pool.shutdown();

    }

    /**
     * 方案1：通过传递参数，解决ThreadLocal的线程隔离问题
     */
    public void handleRequestWithParam(){
        USER_TL.set("userName");

        pool.submit(() -> {
            USER_TL.set("userName");
           try {
               doBusiness();
           }finally {
               USER_TL.remove();
           }
        });

        USER_TL.remove();

        pool.shutdown();
    }
    private void doBusiness() {
        System.out.println(USER_TL.get());
    }

    /**
     * 方案2：使用官方的 InheritableThreadLocal 解决线程隔离
     */
    public void handleRequestWithInheritable(){
        USER_TL_INHERI.set("userName");

        pool.submit(() -> {
            doBusinessWithInheritable();
        });

        USER_TL_INHERI.set("user_B");

        pool.submit(() -> {
            doBusinessWithInheritable();
        });

        pool.shutdown();

        USER_TL_INHERI.remove();
    }
    private void doBusinessWithInheritable() {
        System.out.println(USER_TL_INHERI.get());
    }


    public void handleRequestWithTransmit() throws ExecutionException, InterruptedException {

        USER_TL_Transmit.set("user-A");

        Future<?> f1 = poolWithTtl.submit(() -> {
            doBusinessWithTransmit();
        });

        USER_TL_Transmit.set("user-B");

        Future<?> f2 = poolWithTtl.submit(() -> {
            doBusinessWithTransmit();
        });

        f1.get();
        f2.get();

        poolWithTtl.shutdown();
        USER_TL_Transmit.remove();
    }
    private void doBusinessWithTransmit() {
        System.out.println(USER_TL_Transmit.get());
    }


    public static void main(String[] args) throws ExecutionException, InterruptedException {
        threadLocalContext threadLocalContext = new threadLocalContext();
        // 调用会产生 ThreadLocal 隔离的问题方法
        // threadLocalContext.handleRequestWithError();

        // 方案1：调用 通过手动传参形式解决 ThreadLocal线程隔离问题的方法
        // threadLocalContext.handleRequestWithParam();

        // 方案2：通过 InheritableThreadLocal 解决问题
        // threadLocalContext.handleRequestWithInheritable();

        // 方案3：通过阿里开源的 TransmittableThreadLocal 解决问题
        threadLocalContext.handleRequestWithTransmit();
    }
}
