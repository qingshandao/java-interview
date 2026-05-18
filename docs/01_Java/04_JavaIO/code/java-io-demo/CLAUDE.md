# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是 Java IO 面试知识点的示例代码项目，属于 `java-interview` 文档站点中"Java IO"章节（`docs/01_Java/04_JavaIO/`）的配套演示代码。对应的知识点文档包括：IO 基础知识、IO 设计模式、IO 模型、NIO 核心知识点。

当前项目为 Spring Boot 骨架，Demo 代码待添加。

## 构建与运行

```bash
# 编译
mvn compile

# 运行测试
mvn test

# 运行单个测试
mvn test -Dtest="JavaIoDemoApplicationTests"
```

Demo 类通过 IDE 直接运行 `main` 方法即可，无需启动 Spring Boot 应用。

## 技术栈

- Java 8、Spring Boot 2.6.13
- Spring Web + Spring Data JPA（仅为骨架依赖，Demo 代码中不强制使用）
- Lombok、JUnit 5
- Maven 构建

## 编码约定

- 每个 Demo 类自包含独立的 `main` 方法，通过标准输出展示运行结果
- 源码兼容 Java 8，编译器配置为 Java 8
- `spring-boot-maven-plugin` 配置了 `<skip>true</skip>`，默认不打包可执行 jar
