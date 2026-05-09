package com.gc.multithreaddemo.demos.cas;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class UnsafeCasObjectDemo {
    private volatile String name = "张三";

    private static Unsafe unsafe;

    private static long nameOffset;

    static {
        try {
            // 1.反射拿到 theUnsafe字段（因为 Unsafe() 构造器是私有的，无法直接创建Unsafe对象），并初始化 unsafe 对象
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            // Unsafe 类中，"theUnsafe"字段时static的，不属于某个对象，因此get空对象（null）的值传给 unsafe 对象
            unsafe = (Unsafe) theUnsafe.get(null);

            // 2.获取当前类中 “name” 字段的偏移量
            nameOffset = unsafe.objectFieldOffset(UnsafeCasObjectDemo.class.getDeclaredField("name"));

        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        UnsafeCasObjectDemo demo = new UnsafeCasObjectDemo();

        boolean result = unsafe.compareAndSwapObject(
                demo,
                nameOffset,
                "张三",
                "李四"
        );

        System.out.println(result);
        System.out.println(demo.name);
    }

}
