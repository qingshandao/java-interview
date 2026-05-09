package com.gc.multithreaddemo.demos.future;

import java.util.concurrent.*;

public class FutureDemo2 {
    public static void main(String[] args) {
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(3);

        Future<String> future = fixedThreadPool.submit(() -> {
            try {
                Thread.sleep(5000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return "abc";
        });

        String result = null;
        try {
            result = future.get(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }finally {
            fixedThreadPool.shutdown();
        }

        while (!fixedThreadPool.isTerminated()){}

        System.out.println(result);

    }
}
