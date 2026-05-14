package com.gc.multithreaddemo.demos.aqs;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockConditionDemo {

    /**
     * ReentrantLock
     * 独占模式
     */
    public static void reentrantLockDemo() {
        ReentrantLock lock = new ReentrantLock();

        Runnable task = () -> {
            lock.lock();
            try {
                System.out.println(Thread.currentThread().getName() + " 拿到了ReentrantLock，获取独占锁成功");
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                System.out.println(Thread.currentThread().getName() + " 释放了ReentrantLock");
                lock.unlock();
            }
        };

        System.out.println("\n ======= ReentrantLock（独占锁模式）=========");
        for (int i = 0; i < 5; i++) {
            new Thread(task, "独占锁线程-" + i).start();
        }
    }

    /**
     * Condition 的 await / signal / signalAll
     * 用生产者-消费者模型来演示：
     *   - 队列满时，生产者 await（释放锁并挂起）
     *   - 队列空时，消费者 await
     *   - 生产/消费一次后，signal 对方
     */
    public static void conditionDemo() throws InterruptedException {
        System.out.println("\n ======= Condition（await/signal/signalAll）=========");

        final int capacity = 3;
        final Queue<Integer> queue = new LinkedList<>();
        final ReentrantLock lock = new ReentrantLock();
        // 一把锁可以创建多个 Condition，相当于把等待队列分组
        final Condition notFull  = lock.newCondition();   // 生产者等的条件：队列不满
        final Condition notEmpty = lock.newCondition();   // 消费者等的条件：队列不空

        // 生产者
        Runnable producer = () -> {
            for (int i = 0; i < 5; i++) {
                lock.lock();
                try {
                    // 一定要用 while，不能用 if（防止虚假唤醒 + 唤醒后条件已被别人破坏）
                    while (queue.size() == capacity) {
                        System.out.println(Thread.currentThread().getName() + " 队列已满，await 等待消费...");
                        notFull.await();   // 释放锁 + 挂起，被唤醒后会重新竞争锁
                    }
                    queue.offer(i);
                    System.out.println(Thread.currentThread().getName() + " 生产 " + i + "，当前队列大小=" + queue.size());
                    // 通知消费者：队列里有东西了
                    notEmpty.signal();     // 只唤醒一个等在 notEmpty 上的线程
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    lock.unlock();
                }
            }
        };

        // 消费者
        Runnable consumer = () -> {
            for (int i = 0; i < 5; i++) {
                lock.lock();
                try {
                    while (queue.isEmpty()) {
                        System.out.println(Thread.currentThread().getName() + " 队列为空，await 等待生产...");
                        notEmpty.await();
                    }
                    Integer val = queue.poll();
                    System.out.println(Thread.currentThread().getName() + " 消费 " + val + "，当前队列大小=" + queue.size());
                    notFull.signal();      // 通知生产者：队列腾出位置了
                    Thread.sleep(200);     // 模拟消费慢一点，能看到队列被打满
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    lock.unlock();
                }
            }
        };

        Thread p = new Thread(producer, "生产者");
        Thread c = new Thread(consumer, "消费者");
        p.start();
        c.start();
        p.join();
        c.join();
    }

    /**
     * signalAll 演示：
     * 多个线程都在同一个 Condition 上 await，主线程一次性 signalAll 全部唤醒。
     */
    public static void signalAllDemo() throws InterruptedException {
        System.out.println("\n ======= Condition.signalAll =========");

        final ReentrantLock lock = new ReentrantLock();
        final Condition ready = lock.newCondition();
        final boolean[] started = {false};

        Runnable worker = () -> {
            lock.lock();
            try {
                while (!started[0]) {
                    System.out.println(Thread.currentThread().getName() + " 等待开跑信号...");
                    ready.await();
                }
                System.out.println(Thread.currentThread().getName() + " 收到信号，开始执行！");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        };

        for (int i = 0; i < 4; i++) {
            new Thread(worker, "Worker-" + i).start();
        }

        Thread.sleep(500); // 确保 4 个线程都进入 await
        lock.lock();
        try {
            started[0] = true;
            System.out.println(">>> 主线程发出 signalAll，唤醒所有等待者");
            ready.signalAll();   // 唤醒所有等在 ready 上的线程
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        reentrantLockDemo();
        Thread.sleep(2000); // 等上面跑完
        conditionDemo();
        signalAllDemo();
    }
}
