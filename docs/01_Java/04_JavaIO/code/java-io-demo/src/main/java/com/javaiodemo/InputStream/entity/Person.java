package com.javaiodemo.InputStream.entity;

import java.io.Serializable;

public class Person implements Serializable {

    // 建议显式定义版本号
    private static final long serialVersionUID = 1L;

    private String name;

    private String job;

    // 不想被序列化
    private transient String password;

    public Person(String name, String job, String password) {
        this.name = name;
        this.job = job;
        this.password = password;
    }

    @Override
    public String toString() {
        return "Person{" +
                "name='" + name + '\'' +
                ", job='" + job + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
