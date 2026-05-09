package com.gc.multithreaddemo.demos.atomic;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class AtomicIntegerFieldUpdaterDemo {
    public static void main(String[] args) {
        // 1.创建 AtomicIntegerFieldUpdater 对象
        AtomicIntegerFieldUpdater<Person> newAgeUpdater = AtomicIntegerFieldUpdater.newUpdater(Person.class, "newAge");

        // 2.创建 Person 对象
        Person person = new Person("Sam", 1, 22);

        // 3.打印初始值
        System.out.println("初始值：" + person);

        // 4.newAge 字段自增
        int resultAfterIncrement = newAgeUpdater.incrementAndGet(person);
        System.out.println("\n自增后字段值：" + resultAfterIncrement);
        System.out.println("自增后对象：" + person);

        // 5. newAge 字段增加指定值
        int resultAfterAdd = newAgeUpdater.addAndGet(person, 5);
        System.out.println("\n增加5之后的结果：" + resultAfterAdd);
        System.out.println("增加5之后对象：" + person);

        // 6. newAge 字段判断并指定值
        boolean resultAfterCompare = newAgeUpdater.compareAndSet(person, 28, 88);
        System.out.println("\n判断等于28后，设置为88是否成功：" + resultAfterCompare);
        System.out.println("判断等于28后，设置为88的对象：" + person);

        // 7. newAge 字段判断并指定值
        boolean resultAfterCompareFault = newAgeUpdater.compareAndSet(person, 99, 999);
        System.out.println("\n判断等于99后，设置为999是否成功：" + resultAfterCompareFault);
        System.out.println("判断等于99后，设置为999的对象：" + person);

    }
}
