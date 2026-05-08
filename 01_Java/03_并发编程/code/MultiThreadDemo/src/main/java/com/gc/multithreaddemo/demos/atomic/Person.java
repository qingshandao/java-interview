package com.gc.multithreaddemo.demos.atomic;

public class Person {
    private String name;
    private Integer age;

    volatile int newAge;    // AtomicIntegerFieldUpdater 测试属性

    public Person(String name, Integer age) {
        this.name = name;
        this.age = age;
    }

    public Person(String name, Integer age, int newAge) {
        this.name = name;
        this.age = age;
        this.newAge = newAge;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public int getNewAge() {
        return newAge;
    }

    public void setNewAge(int newAge) {
        this.newAge = newAge;
    }

    @Override
    public String toString() {
        return "Person{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", newAge=" + newAge +
                '}';
    }
}
