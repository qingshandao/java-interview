# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是 Java 并发编程面试知识点的示例代码项目，属于 `30_Java-interview` 文档站点中"并发编程"章节的配套演示代码。每个类都是独立可运行的 Demo，通过 `main` 方法直接运行来演示特定的并发概念。

## 构建与运行

```bash
# 编译（Java 17 编译器，源码兼容 Java 8）
mvn compile

# 运行单个 Demo（通过 exec 插件或 IDE 直接运行 main 方法）
mvn exec:java -Dexec.mainClass="com.gc.multithreaddemo.demos.aqs.CountDownLatchExample1"

# 运行测试
mvn test

# 打包
mvn clean package -DskipTests
```

**注意**：`spring-boot-maven-plugin` 配置了 `<skip>true</skip>`，默认不会打可执行 jar。若需要启动 Spring Boot 应用（用于 Actuator 线程池监控），需先移除该配置。

## 技术栈

- Spring Boot 2.6.13 + Spring Web + Spring Actuator + Micrometer
- MyBatis + MySQL（仅为演示配置，Demo 代码中未实际使用数据库）
- Lombok、Guava、Transmittable-Thread-Local
- JUnit 5（Spring Boot Test）

## 项目结构

```
src/main/java/com/gc/multithreaddemo/
├── MultiThreadDemoApplication.java    # Spring Boot 入口
└── demos/
    ├── aqs/               # AQS 同步器：CountDownLatch、CyclicBarrier、Semaphore、ReentrantLock+Condition、独占/共享模式
    ├── atomic/            # 原子类：AtomicReference、AtomicStampedReference、AtomicIntegerFieldUpdater
    ├── cas/               # CAS 底层：Unsafe
    ├── contextSwitch/     # 上下文切换开销演示
    ├── createThread/      # 线程创建方式：Thread、Runnable、Callable、线程池（含 submit/execute 异常处理）
    ├── future/            # Future 异步任务
    ├── reentrantLock/     # ReentrantLock：公平锁、Condition、tryLock 避免死锁
    ├── synchronizedDemo/  # synchronized：对象锁、类锁、wait/notify
    ├── threadFactory/     # 自定义 ThreadFactory
    ├── threadLocal/       # ThreadLocal、InheritableThreadLocal、TTL、GC 演示
    └── threadPool/        # 线程池核心参数、拒绝策略、preStart、优先级、Actuator 监控
        └── actuator/      # Spring Actuator 监控线程池状态
            ├── config/    # ThreadPoolConfig、ThreadPoolMetricsConfig
            ├── controller/# TestController、ThreadPoolMonitorController
            ├── factory/   # NamedThreadFactory
            └── monitor/   # 监控相关
```

每个 package 下的 Java 类都是独立的 Demo 程序，各自包含 `main` 方法，通过标准输出打印结果来演示并发行为。

## 编码约定

- JDK 兼容性：源码兼容 Java 8（可用的 java.util.concurrent API），编译器使用 Java 17（需要 `--add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED` 以使用 `Unsafe`）
- 每个 Demo 类力求自包含，不依赖其他 Demo 类
- 线程池 Demo 中会显式 `shutdown()` 线程池
- 测试文件仅在 `src/test` 中，目前只有一个 `contextLoads` 空测试
- 所有的函数必须有注释说明
