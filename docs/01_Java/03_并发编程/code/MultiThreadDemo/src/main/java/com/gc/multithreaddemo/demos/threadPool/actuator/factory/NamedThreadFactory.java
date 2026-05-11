package com.gc.multithreaddemo.demos.threadPool.actuator.factory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {

    private final String prefix;

    private final AtomicInteger atomicInteger =
            new AtomicInteger(1);

    public NamedThreadFactory(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable r) {

        return new Thread(
                r,
                prefix + "-" + atomicInteger.getAndIncrement()
        );
    }
}
