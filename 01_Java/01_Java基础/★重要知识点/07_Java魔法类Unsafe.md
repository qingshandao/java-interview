# 一、魔法类 Unsafe 详解

> 本文整理完善自下面这两篇优秀的文章：
>
> - [Java 魔法类：Unsafe 应用解析 - 美团技术团队 -2019](https://tech.meituan.com/2019/02/14/talk-about-java-magic-class-unsafe.html)
> - [Java 双刃剑之 Unsafe 类详解 - 码农参上 - 2021](https://xie.infoq.cn/article/8b6ed4195e475bfb32dacc5cb)

阅读过 JUC 源码的同学，一定会发现很多并发工具类都调用了一个叫做 `Unsafe` 的类。

那这个类主要是用来干什么的呢？有什么使用场景呢？这篇文章就带你搞清楚！

## 1、Unsafe 介绍

`Unsafe` 是位于 `sun.misc` 包下的一个类，主要提供一些用于执行低级别、不安全操作的方法，如直接访问系统内存资源、自主管理内存资源等，这些方法在提升 Java 运行效率、增强 Java 语言底层资源操作能力方面起到了很大的作用。但由于 `Unsafe` 类使 Java 语言拥有了类似 C 语言指针一样操作内存空间的能力，这无疑也增加了程序发生相关指针问题的风险。在程序中过度、不正确使用 `Unsafe` 类会使得程序出错的概率变大，使得 Java 这种安全的语言变得不再“安全”，因此对 `Unsafe` 的使用一定要慎重。

另外，`Unsafe` 提供的这些功能的实现需要依赖本地方法（Native Method）。你可以将本地方法看作是 Java 中使用其他编程语言编写的方法。本地方法使用 **`native`** 关键字修饰，Java 代码中只是声明方法头，具体的实现则交给 **本地代码**。

<img src="./../assets/Unsafe_Intro.png" style="zoom:80%;" />

**为什么要使用本地方法呢？**

1. 需要用到 Java 中不具备的依赖于操作系统的特性，Java 在实现跨平台的同时要实现对底层的控制，需要借助其他语言发挥作用。
2. 对于其他语言已经完成的一些现成功能，可以使用 Java 直接调用。
3. 程序对时间敏感或对性能要求非常高时，有必要使用更加底层的语言，例如 C/C++甚至是汇编。

在 JUC 包的很多并发工具类在实现并发机制时，都调用了本地方法，通过它们打破了 Java 运行时的界限，能够接触到操作系统底层的某些功能。对于同一本地方法，不同的操作系统可能会通过不同的方式来实现，但是对于使用者来说是透明的，最终都会得到相同的结果。

## 2、Unsafe 创建

`sun.misc.Unsafe` 部分源码如下：

```java
public final class Unsafe {
  // 单例对象
  private static final Unsafe theUnsafe;
  ......
  private Unsafe() {
  }
  @CallerSensitive
  public static Unsafe getUnsafe() {
    Class var0 = Reflection.getCallerClass();
    // 仅在引导类加载器`BootstrapClassLoader`加载时才合法
    if(!VM.isSystemDomainLoader(var0.getClassLoader())) {
      throw new SecurityException("Unsafe");
    } else {
      return theUnsafe;
    }
  }
}
```

`Unsafe` 类为一单例实现，提供静态方法 `getUnsafe` 获取 `Unsafe`实例。这个看上去貌似可以用来获取 `Unsafe` 实例。但是，当我们直接调用这个静态方法的时候，会抛出 `SecurityException` 异常：

```java
Exception in thread "main" java.lang.SecurityException: Unsafe
 at sun.misc.Unsafe.getUnsafe(Unsafe.java:90)
 at com.cn.test.GetUnsafeTest.main(GetUnsafeTest.java:12)
```

**为什么 `public static` 方法无法被直接调用呢？**

这是因为在`getUnsafe`方法中，会对调用者的`classLoader`进行检查，判断当前类是否由`Bootstrap classLoader`加载，如果不是的话那么就会抛出一个`SecurityException`异常。也就是说，只有由**启动类加载器加载的类**，才能够调用 Unsafe 类中的方法，来防止这些方法在不可信的代码中被调用。

> ✅ 什么是“启动类加载器加载的类”？
>
> 启动类加载器（Bootstrap ClassLoader）
> 这是 Java 中的最顶层的类加载器，由 JVM 本身实现（不是 Java 写的），用于加载 JDK 核心类库，比如：
>
> - java.lang.*
>
> - java.util.*
>
> - sun.misc.*
>
> - jdk.internal.*
>
> - java.nio.*
>
> 它加载的类通常位于：
>
> - JAVA_HOME/lib/ 目录（如 rt.jar 或模块化后的 jmods/）
>
> - Java 9+ 中的模块系统（jrt:/ 虚拟路径）
>
> 特点：
>
> - 它是 native 实现的，没有 Java 对象实例（ClassLoader 为 null）
>
> - 用于加载最基础的系统类
>
> - 由 JVM 在启动时自动初始化
>
> ✅ 如何判断某个类是由哪个类加载器加载的？
>
> 可以打印它的 ClassLoader：
>
> ```java
> System.out.println(SomeClass.class.getClassLoader());
> ```
>
> 示例：
>
> ```java
> System.out.println(java.lang.String.class.getClassLoader()); 
> // 输出: null  （说明是 Bootstrap 加载器加载的）
> 
> System.out.println(MyClass.class.getClassLoader()); 
> // 输出: sun.misc.Launcher$AppClassLoader@xxxx
> ```
>
> 

**为什么要对 Unsafe 类进行这么谨慎的使用限制呢?**

`Unsafe` 提供的功能过于底层（如直接访问系统内存资源、自主管理内存资源等），安全隐患也比较大，使用不当的话，很容易出现很严重的问题。

**如若想使用 `Unsafe` 这个类的话，应该如何获取其实例呢？**

这里介绍两个可行的方案。

1、利用反射获得 Unsafe 类中已经实例化完成的单例对象 `theUnsafe` 。

```java
private static Unsafe reflectGetUnsafe() {
    try {
      Field field = Unsafe.class.getDeclaredField("theUnsafe");
      field.setAccessible(true);
      return (Unsafe) field.get(null);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      return null;
    }
}
```

2、从`getUnsafe`方法的使用限制条件出发，通过 Java 命令行命令`-Xbootclasspath/a`把调用 Unsafe 相关方法的类 A 所在 jar 包路径追加到默认的 bootstrap 路径中，使得 A 被引导类加载器加载，从而通过`Unsafe.getUnsafe`方法安全的获取 Unsafe 实例。

```java
java -Xbootclasspath/a: ${path}   // 其中path为调用Unsafe相关方法的类所在jar包路径
```

## 3、Unsafe 功能

概括的来说，`Unsafe` 类实现功能可以被分为下面 8 类：

1. 内存操作
2. 内存屏障
3. 对象操作
4. 数据操作
5. CAS 操作
6. 线程调度
7. Class 操作
8. 系统信息

### 3.1、内存操作

#### 3.1.1、介绍

如果写过 C 或者 C++ ，一定对内存操作不会陌生，而在 Java 中是不允许直接对内存进行操作的，对象内存的分配和回收都是由 JVM 自己实现的。但是在 `Unsafe` 中，提供的下列接口可以直接进行内存操作：

- 分配新的本地空间

  ```java
  public native long allocateMemory(long bytes);
  ```

  | 参数    | 类型   | 含义               |
  | ------- | ------ | ------------------ |
  | `bytes` | `long` | 要分配的内存字节数 |

- 重新调整内存空间的大小

  ```java
  public native long reallocateMemory(long address, long bytes);
  ```

  | 参数名    | 类型   | 说明                                                 |
  | --------- | ------ | ---------------------------------------------------- |
  | `address` | `long` | 原来通过 `allocateMemory` 分配的堆外内存地址（指针） |
  | `bytes`   | `long` | 重新分配的新内存大小（以字节为单位）                 |

- 将内存设置为指定值

  ```java
  public native void setMemory(Object o, long offset, long bytes, byte value);
  ```

  | 参数     | 类型     | 含义                                                         |
  | -------- | -------- | ------------------------------------------------------------ |
  | `o`      | `Object` | 内存所属的 Java 对象，如果为 `null` 表示直接在堆外内存中操作（即 off-heap） |
  | `offset` | `long`   | 要操作的内存起始偏移量（相对于 `o` 的偏移地址，或者是直接的绝对地址） |
  | `bytes`  | `long`   | 要填充的字节长度                                             |
  | `value`  | `byte`   | 要填充的字节值                                               |

- 内存拷贝

  ```java
  public native void copyMemory(Object srcBase, long srcOffset,Object destBase, long destOffset,long bytes);
  ```

  | 参数名       | 类型     | 含义                                                        |
  | ------------ | -------- | ----------------------------------------------------------- |
  | `srcBase`    | `Object` | **源对象**。如果是 `null`，表示操作的是堆外内存（off-heap） |
  | `srcOffset`  | `long`   | 源对象的字段偏移（或堆外内存地址）                          |
  | `destBase`   | `Object` | **目标对象**。如果是 `null`，表示写入的是堆外内存           |
  | `destOffset` | `long`   | 目标对象的字段偏移（或堆外内存地址）                        |
  | `bytes`      | `long`   | 要拷贝的字节数                                              |

- 清除内存

  ```
  public native void freeMemory(long address);
  ```

- 从指定的内存地址（堆外）读取一个 int 值（即连续的4个字节）

  ```java
  public native int getInt(long address);
  ```

  | 参数名    | 类型   | 含义                                       |
  | --------- | ------ | ------------------------------------------ |
  | `address` | `long` | 要读取的内存地址（**绝对地址，堆外内存**） |
  | 返回值    | `int`  | 从指定地址读取的 4 字节整数值              |

#### 3.1.2、使用下面的代码进行测试：

```java
/**
 * 测试 unsafe 的常见方法
 * @param unsafe
 */
private static void memoryTest(Unsafe unsafe) {
    int size = 4;
    long addr = unsafe.allocateMemory(size);	// 分配内存空间
    long addr3 = unsafe.reallocateMemory(addr, size * 2);	// 重新调整内存空间大小
    System.out.println("addr: "+addr);
    System.out.println("addr3: "+addr3);
    try {
        unsafe.setMemory(null,addr ,size,(byte)1);	// 将内存设定为指定值【从内存地址 addr 开始，连续 size 个字节的区域都循环填充为 (byte) 1】
        for (int i = 0; i < 2; i++) {
            unsafe.copyMemory(null,addr,null,addr3+size*i,4);	// 拷贝内存
        }
        System.out.println(unsafe.getInt(addr));
        System.out.println(unsafe.getLong(addr3));
    }finally {
        unsafe.freeMemory(addr);	// 释放内存
        unsafe.freeMemory(addr3);
    }
}

public static void main(String[] args) {
    // 获取 unsafe 对象
    Unsafe unsafe = reflectGetUnsafe();
    // 测试常见方法
    memoryTest(unsafe);

}

/**
 * 通过反射，获取 Unsafe 对象
 *
 * @return
 */
private static Unsafe reflectGetUnsafe() {
    try {
        // 获取 单例成员对象 theUnsafe
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    } catch (Exception e) {
        System.out.println(e.getMessage());
        return null;
    }
}
```

- 输出结果：

  ```java
  addr: 2433733895744
  addr3: 2433733894944
  16843009
  72340172838076673
  ```

  分析一下运行结果，首先使用`allocateMemory`方法申请 4 字节长度的内存空间，调用`setMemory`方法向每个字节写入内容为`byte`类型的 1，当使用 Unsafe 调用`getInt`方法时，因为一个`int`型变量占 4 个字节，会一次性读取 4 个字节，组成一个`int`的值，对应的十进制结果为 16843009。

  可以通过下图理解这个过程：

  <img src="./../assets/unsafe_memory_examp1.png" style="zoom:80%;" />

在代码中调用`reallocateMemory`方法重新分配了一块 8 字节长度的内存空间，通过比较`addr`和`addr3`可以看到和之前申请的内存地址是不同的。在代码中的第二个 for 循环里，调用`copyMemory`方法进行了两次内存的拷贝，每次拷贝内存地址`addr`开始的 4 个字节，分别拷贝到以`addr3`和`addr3+4`开始的内存空间上：

<img src="./../assets/unsafe_memory_example2.png" style="zoom:80%;" />

拷贝完成后，使用`getLong`方法一次性读取 8 个字节，得到`long`类型的值为 72340172838076673。

需要注意，通过这种方式分配的内存属于 堆外内存 ，是无法进行垃圾回收的，需要我们把这些内存当做一种资源去手动调用`freeMemory`方法进行释放，否则会产生内存泄漏。通用的操作内存方式是在`try`中执行对内存的操作，最终在`finally`块中进行内存的释放。

**为什么要使用堆外内存？**

- 对垃圾回收停顿的改善。由于堆外内存是直接受操作系统管理而不是 JVM，所以当我们使用堆外内存时，即可保持较小的堆内内存规模。从而在 GC 时减少回收停顿对于应用的影响。
- 提升程序 I/O 操作的性能。通常在 I/O 通信过程中，会存在堆内内存到堆外内存的数据拷贝操作，对于需要频繁进行内存间数据拷贝且生命周期较短的暂存数据，都建议存储到堆外内存。

#### 3.1.3、典型应用

`DirectByteBuffer` 是 Java 用于实现堆外内存的一个重要类，通常用在通信过程中做缓冲池，如在 Netty、MINA 等 NIO 框架中应用广泛。`DirectByteBuffer` 对于堆外内存的创建、使用、销毁等逻辑均由 Unsafe 提供的堆外内存 API 来实现。

下图为 `DirectByteBuffer` 构造函数，创建 `DirectByteBuffer` 的时候，通过 `Unsafe.allocateMemory` 分配内存、`Unsafe.setMemory` 进行内存初始化，而后构建 `Cleaner` 对象用于跟踪 `DirectByteBuffer` 对象的垃圾回收，以实现当 `DirectByteBuffer` 被垃圾回收时，分配的堆外内存一起被释放。

```java
DirectByteBuffer(int cap) {                   // package-private

    super(-1, 0, cap, cap);
    boolean pa = VM.isDirectMemoryPageAligned();
    int ps = Bits.pageSize();
    long size = Math.max(1L, (long)cap + (pa ? ps : 0));
    Bits.reserveMemory(size, cap);

    long base = 0;
    try {
        // 分配内存并返回基地址
        base = unsafe.allocateMemory(size);
    } catch (OutOfMemoryError x) {
        Bits.unreserveMemory(size, cap);
        throw x;
    }
    // 内存初始化
    unsafe.setMemory(base, size, (byte) 0);
    if (pa && (base % ps != 0)) {
        // Round up to page boundary
        address = base + ps - (base & (ps - 1));
    } else {
        address = base;
    }
    // 跟踪 DirectByteBuffer 对象的垃圾回收，以实现堆外内存释放
    cleaner = Cleaner.create(this, new Deallocator(base, size, cap));
    att = null;
}
```

### 3.2、内存屏障

