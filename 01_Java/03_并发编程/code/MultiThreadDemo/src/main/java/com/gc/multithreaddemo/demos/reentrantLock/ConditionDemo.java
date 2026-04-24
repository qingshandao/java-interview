package com.gc.multithreaddemo.demos.reentrantLock;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ConditionDemo {

    private final Queue<Integer> queue = new LinkedList();
    private final int capacity = 5;
    private final ReentrantLock lock = new ReentrantLock();

    // 两组线程等待队列
    private final Condition notFull = lock.newCondition();  // 生产者线程等待队列
    private final Condition notEmpty = lock.newCondition(); // 消费者线程等待队列

    // 生产
    public void produce(int value) throws InterruptedException {
        // 1.加锁
        lock.lock();

       try {
           // 2.判断队列是否满
           while (queue.size() == capacity){
               System.out.println("队列满，生产者等待中。。。");
               // 当前线程计入生产者线程等待队列
               notFull.await();
           }

           // 3.队列不满，开始生产
           queue.offer(value);
           System.out.println("生产：" + value);
           // 4.唤醒消费者队列
           notEmpty.signal();
       }finally {
           lock.unlock();
       }
    }

    // 消费
    public Integer consume() throws InterruptedException{
        // 1.加锁
        lock.lock();

        try {
            while (queue.isEmpty()){
                System.out.println("队列空，消费者线程等待中...");
                // 2.消费者线程等待中
                notEmpty.await();
            }

            // 3.队列不空，开始消费
            Integer poll = queue.poll();
            System.out.println("消费：" + poll);

            // 4.唤醒生产者队列
            notFull.signal();

            return poll;
        }finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) {
        ConditionDemo conditionDemo = new ConditionDemo();

        // 生产者线程
        new Thread(() -> {
            int i = 0;
            while (true){
                try {
                    conditionDemo.produce(i++);
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        // 消费者线程
        new Thread(() -> {
            while (true){
                try {
                    Integer consume = conditionDemo.consume();
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }
}
