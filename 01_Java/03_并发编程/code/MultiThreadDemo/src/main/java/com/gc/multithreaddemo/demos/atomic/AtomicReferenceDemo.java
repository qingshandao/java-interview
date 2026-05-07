package com.gc.multithreaddemo.demos.atomic;

import java.util.concurrent.atomic.AtomicReference;

public class AtomicReferenceDemo {
    public static void main(String[] args) {

        // 创建 AtomicReference 对象并设置初始值
        AtomicReference<Person> ar = new AtomicReference<>(new Person("SnailClimb", 22));

        // 打印初始值
        System.out.println("Initial Person: " + ar.get().toString());

        // 更新值
        Person updatePerson = new Person("Daisy", 20);
        ar.compareAndSet(ar.get(), updatePerson);

        // 打印更新后的值
        System.out.println("Updated Person: " + ar.get().toString());

        // 尝试再次更新
        Person anotherUpdatePerson = new Person("John", 30);
        boolean isUpdated = ar.compareAndSet(updatePerson, anotherUpdatePerson);

        // 打印是否更新成功及最终值
        System.out.println("Second Update Success: " + isUpdated);
        System.out.println("Final Person: " + ar.get().toString());
    }
}
