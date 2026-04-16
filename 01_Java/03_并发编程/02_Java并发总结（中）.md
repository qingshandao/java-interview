# 一、⭐️JMM(Java 内存模型)

JMM（Java 内存模型）相关的问题比较多，也比较重要，于是我单独抽了一篇文章来总结 JMM 相关的知识点和问题：[JMM（Java 内存模型）详解](./★重要知识点\03_JMM（Java内存模型）详解.md) 。



# 二、⭐️volatile 关键字
## 1、如何保证变量的可见性？

在 Java 中，`volatile` 关键字可以保证变量的可见性，如果我们将变量声明为 **`volatile`** ，这就指示 JVM，这个变量是共享且不稳定的，每次使用它都到主存中进行读取。

![](./assets/jmm.png)

`volatile` 关键字其实并非是 Java 语言特有的，在 C 语言里也有，它最原始的意义就是禁用 CPU 缓存。如果我们将一个变量使用 `volatile` 修饰，这就指示 编译器，这个变量是共享且不稳定的，每次使用它都到主存中进行读取。

`volatile` 关键字能**保证**数据的 **可见性**，但**不能保证**数据的 **原子性**。`synchronized` 关键字两者都能保证。

> 这句话的核心在于：**“一个线程对共享变量的修改，其他线程能不能看到（可见性）” 和 “操作是不是一步完成不会被打断（原子性）” 是两回事。**
>
> ### 一、volatile：保证可见性，但不保证原子性
>
> #### ✅ 示例：计数器问题
>
> ```java
> class Counter {
>     volatile int count = 0;
> 
>     public void increment() {
>         count++;   // 非原子操作！
>     }
> }
> ```
>
> 假设有 10 个线程，每个线程执行 1000 次 `increment()`：
>
> ```java
> Counter counter = new Counter();
> 
> for (int i = 0; i < 10; i++) {
>     new Thread(() -> {
>         for (int j = 0; j < 1000; j++) {
>             counter.increment();
>         }
>     }).start();
> }
> ```
>
> ❗以为结果是：
>
> ```java
> 10000
> ```
>
> ❗实际可能是：
>
> ```java
> 8732 / 9210 / 9988（不确定）
> ```
>
> #### 🔍 为什么 volatile 不行？
>
> 因为：
>
> ```java
> count++;
> ```
>
> **实际上是 3 步操作：**
>
> 1. 读取 count
> 2. +1
> 3. 写回 count
>
> 👉 线程A和线程B可能这样执行：
>
> ```java
> 线程A读取 count = 0
> 线程B读取 count = 0
> 线程A写回 count = 1
> 线程B写回 count = 1
> ```
>
> 💥 **结果丢失了一次加法**
>
> #### ✅ volatile 能保证什么？
>
> - 每次读取都是**最新值（主内存）**
> - 不会用线程缓存的旧值
>
> 👉 但它**无法阻止多个线程同时修改**
>
> 
>
> ### 二、synchronized：同时保证可见性 + 原子性
>
> #### ✅ 改造上面的代码
>
> ```java
> class Counter {
>     int count = 0;
> 
>     public synchronized void increment() {
>         count++;
>     }
> }
> ```
>
> #### 🔒 synchronized 做了什么？
>
> ##### 1️⃣ 原子性保证
>
> 同一时间：
>
> 👉 **只能有一个线程进入 increment() 方法**
>
> ```
> 线程A执行完 → 线程B才能进
> ```
>
> ➡️ 不会出现“同时修改”的问题
>
> ------
>
> ##### 2️⃣ 可见性保证
>
> - 线程进入 synchronized 时：读取主内存最新值
>
> - 线程退出 synchronized 时：刷新到主内存
>
>   > 👉 **“synchronized中的变量都会读主内存”** 吗？
>   >
>   > - **进入 synchronized 时，会清空工作内存，从主内存重新读取 共享变量**
>   > - **退出 synchronized 时，会把修改刷新回主内存**
>   >
>   > 所以结果表现为：
>   >
>   > ✅ 能看到“最新值”（可见性）
>   > 但❗不是“每次读取变量都直接访问主内存”
>
> ➡️ 所有线程看到的都是最新结果

## 2、如何禁止指令重排序？

**在 Java 中，`volatile` 关键字除了可以保证变量的可见性，还有一个重要的作用就是防止 JVM 的指令重排序。** 如果我们将变量声明为 **`volatile`** ，在对这个变量进行读写操作的时候，会通过插入特定的 **内存屏障** 的方式来禁止指令重排序。

在 Java 中，`Unsafe` 类提供了三个开箱即用的内存屏障相关的方法，屏蔽了操作系统底层的差异：

```java
public native void loadFence();
public native void storeFence();
public native void fullFence();
```

理论上来说，通过这个三个方法也可以实现和`volatile`禁止重排序一样的效果，只是会麻烦一些。

### 2.1、4 种内存屏障类型

JMM（Java 内存模型）定义了 4 种内存屏障（Memory Barrier），用于控制特定条件下的指令重排序和内存可见性：

| 屏障类型       | 指令示例                     | 说明                                                         |
| -------------- | ---------------------------- | ------------------------------------------------------------ |
| **LoadLoad**   | `Load1; LoadLoad; Load2`     | 保证 `Load1` 的读取操作在 `Load2` 及其后续读取操作之前完成   |
| **StoreStore** | `Store1; StoreStore; Store2` | 保证 `Store1` 的写入操作对其他处理器可见（刷新到内存），先于 `Store2` 及其后续写入操作 |
| **LoadStore**  | `Load1; LoadStore; Store2`   | 保证 `Load1` 的读取操作在 `Store2` 及其后续写入操作刷新到内存之前完成 |
| **StoreLoad**  | `Store1; StoreLoad; Load2`   | 保证 `Store1` 的写入操作对其他处理器可见，先于 `Load2` 及其后续读取操作。`StoreLoad` 屏障的开销是四种屏障中最大的，它同时具有其他三种屏障的效果，因此也称为 **全能屏障（Full Barrier）** |

### 2.2、volatile 读写操作的内存屏障插入策略

JMM 针对编译器制定了 `volatile` 读写操作的内存屏障插入策略，以确保在任意处理器平台上都能获得正确的 volatile 内存语义：

**volatile 写 操作的内存屏障插入策略：**

在每个 volatile 写操作的 **前面** 插入一个 `StoreStore` 屏障，在 **后面** 插入一个 `StoreLoad` 屏障。

```
StoreStore 屏障
volatile 写操作
StoreLoad 屏障
```

- 前面的 `StoreStore` 屏障：保证在 volatile 写之前，其前面的所有普通写操作已经对任意处理器可见（刷新到主内存）。

- 后面的 `StoreLoad` 屏障：保证 volatile 写之后，其写入的值对后续的 volatile 读/写操作可见。这是开销最大的屏障，但也是最关键的——它避免了 volatile 写与后面可能有的 volatile 读/写操作发生重排序。

**volatile 读 操作的内存屏障插入策略：**

在每个 volatile 读操作的 **后面** 插入一个 `LoadLoad` 屏障和一个 `LoadStore` 屏障。

```
volatile 读操作
LoadLoad 屏障
LoadStore 屏障
```

- `LoadLoad` 屏障：保证 volatile 读之后的普通读操作不会被重排序到 volatile 读之前。
- `LoadStore` 屏障：保证 volatile 读之后的普通写操作不会被重排序到 volatile 读之前。

这样一来，volatile 写-读的组合就建立了一个类似于 **锁的释放-获取** 的语义：**volatile 写操作之前的所有操作结果，对于后续对该 volatile 变量的读操作之后的所有操作都是可见的。**

下面我以一个常见的面试题为例讲解一下 `volatile` 关键字禁止指令重排序的效果。

面试中面试官经常会说：“单例模式了解吗？来给我手写一下！给我解释一下双重检验锁方式实现单例模式的原理呗！

**双重校验（DCL）锁实现对象单例（线程安全）**：

```java
public class Singleton {

    private volatile static Singleton uniqueInstance;

    private Singleton() {
    }

    public static Singleton getUniqueInstance() {
       //先判断对象是否已经实例过，没有实例化过才进入加锁代码
        if (uniqueInstance == null) {
            //类对象加锁
            synchronized (Singleton.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new Singleton();
                }
            }
        }
        return uniqueInstance;
    }
}
```

`uniqueInstance` 采用 `volatile` 关键字修饰也是很有必要的， `uniqueInstance = new Singleton();` 这段代码其实是分为三步执行：

1. 为 `uniqueInstance` 分配内存空间
2. 初始化 `uniqueInstance`
3. 将 `uniqueInstance` 指向分配的内存地址

但是由于 JVM 具有指令重排的特性，执行顺序有可能变成 1->3->2。指令重排在单线程环境下不会出现问题，但是在多线程环境下会导致一个线程获得还没有初始化的实例。例如，线程 T1 执行了 1 和 3，此时 T2 调用 `getUniqueInstance`() 后发现 `uniqueInstance` 不为空，因此返回 `uniqueInstance`，但此时 `uniqueInstance` 还未被初始化。

**❗问题本质是：** 👉 **“对象引用已经对其他线程可见，但对象还没初始化完”**

> ## 对于多线程中创建对象的思考
>
> ### 一、结论
>
> 👉 **多线程“创建对象”本身不危险，危险的是“对象是否被共享（发布）”**
>
> ### 二、分两种情况看（这是核心）
>
> #### ✅ 情况1：多线程各自创建，各自使用（安全）
>
> ```java
> new Thread(() -> {
>     Singleton s = new Singleton();
>     // 只在当前线程用
> }).start();
> ```
>
> 👉 即使发生指令重排：
>
> ```
> 1. 分配内存
> 3. 引用指向内存
> 2. 初始化对象
> ```
>
> 📌 也没问题，因为：
>
> - 这个对象**没有被共享**
> - 没有其他线程能访问它
>
> ✔️ 所以不会出现“别人拿到半初始化对象”
>
> #### ❗情况2：创建后被其他线程访问（危险）
>
> ```java
> class Demo {
>     static Singleton instance;
> 
>     static void create() {
>         instance = new Singleton(); // ⚠️ 可能发生重排
>     }
> 
>     static void use() {
>         if (instance != null) {
>             instance.doSomething(); // 💥 可能出问题
>         }
>     }
> }
> ```
>
> ### 三、关键点总结（非常重要）
>
> 👉 **是否需要 volatile，不取决于“是不是多线程创建”**
>
> 👉 而取决于：
>
> ```
> 对象有没有被“发布（共享）”
> ```
>
> ### ⚠四、什么叫“发布”（面试高频点）
>
> 👉 一个对象被其他线程访问，就叫发布：
>
> ### 常见发布方式：
>
> - 赋值给 `static` 变量 ✅
> - 赋值给成员变量（被多个线程访问）✅
> - 放进集合（如 List / Map）✅
> - 作为方法返回值返回 ✅
>
> ### 五、什么时候必须考虑 volatile / synchronized？
>
> 👉 满足这三个条件：
>
> ```
> 1. 多线程环境
> 2. 延迟初始化（不是一次性初始化）
> 3. 对象被共享（发布）
> ```
>
> ✔️ 就必须考虑：
>
> - `volatile`
> - `synchronized`
> - 或其他安全发布方式
>
> ### 六、进阶
>
> 除了 `volatile` 和 `synchronized`，还有几种**天然安全发布方式**：
>
> - final 字段（有初始化安全性）
> - 静态初始化（类加载阶段）
> - 枚举单例
> - 线程安全容器（如 ConcurrentHashMap）

### 2.3、从内存屏障角度理解 DCL 必须使用 volatile

上面从指令重排序的角度解释了 DCL（**Double Check Lock** 双重检查锁定） 单例中 `uniqueInstance` 为什么需要 `volatile` 修饰。下面从内存屏障的角度进一步分析 `volatile` 是如何解决这个问题的。

`uniqueInstance = new Singleton();` 这行代码的三个步骤（分配内存、初始化对象、赋值引用）中，如果不加 `volatile`，步骤 2 和步骤 3 可能会被重排序为 1→3→2。加了 `volatile` 之后，由于 `uniqueInstance` 是 volatile 变量，对它的写操作（步骤 3：将引用赋值给 `uniqueInstance`）会按照前面介绍的 volatile 写的内存屏障插入策略来处理：

1. 在 volatile 写 **之前** 插入 `StoreStore` 屏障：保证步骤 1（分配内存）和步骤 2（初始化对象）的写操作在步骤 3（赋值引用）之前完成，**禁止了步骤 2 和步骤 3 的重排序**。
2. 在 volatile 写 **之后** 插入 `StoreLoad` 屏障：保证步骤 3 的写入结果对其他线程立即可见。

这样，当线程 T2 读取 `uniqueInstance` 时（volatile 读），如果发现 `uniqueInstance != null`，那么可以保证该对象一定已经被完全初始化了。

## 3、volatile 与 happens-before 的关系

JMM 中的 happens-before 原则是判断数据是否存在竞争、线程是否安全的重要依据。`volatile` 变量的读写操作与 happens-before 原则有着密切的关系。

> 关于 happens-before 原则的详细介绍，可以参考 [JMM（Java 内存模型）详解](https://javaguide.cn/java/concurrent/jmm.html) 这篇文章。

happens-before 原则中与 `volatile` 直接相关的是 **volatile 变量规则**：

> **对一个 volatile 变量的写操作 happens-before 于后续对该 volatile 变量的读操作。**

也就是说，如果线程 A 写入了一个 volatile 变量，线程 B 随后读取了同一个 volatile 变量，那么线程 A 在写入 volatile 变量之前所做的所有修改（包括对非 volatile 变量的修改），对线程 B 都是可见的。

这个规则配合 happens-before 的 **传递性规则**（如果 A happens-before B，B happens-before C，那么 A happens-before C），可以实现一种轻量级的线程间通信。下面通过一个示例来说明：

```java
public class VolatileHappensBeforeDemo {
    private int a = 0;
    private int b = 0;
    private volatile boolean flag = false;

    // 线程 A 执行
    public void writer() {
        a = 1;           // 操作1：普通写
        b = 2;           // 操作2：普通写
        flag = true;     // 操作3：volatile 写
    }

    // 线程 B 执行
    public void reader() {
        if (flag) {      // 操作4：volatile 读
            int x = a;   // 操作5：普通读，x 一定等于 1
            int y = b;   // 操作6：普通读，y 一定等于 2
            System.out.println("x=" + x + ", y=" + y);
        }
    }
}
```

上面代码中，happens-before 关系链如下：

1. 操作1、操作2 happens-before 操作3（**程序顺序规则**：同一线程中，前面的操作 happens-before 后面的操作）
2. 操作3 happens-before 操作4（**volatile 变量规则**：volatile 写 happens-before volatile 读）
3. 操作4 happens-before 操作5、操作6（**程序顺序规则**）

根据 **传递性**：操作1、操作2 happens-before 操作5、操作6。

因此，当线程 B 在操作4 读取到 `flag == true` 时，线程 A 在操作3 之前对 `a` 和 `b` 的修改对线程 B 一定是可见的。这里的关键在于：**volatile 变量的写-读操作，不仅保证了 volatile 变量本身的可见性，还通过 happens-before 的传递性"顺带"保证了其前后普通变量的可见性。**

这也解释了为什么在实际开发中，`volatile` 经常被用作 **状态标志位**（如上面例子中的 `flag`），它可以在不使用锁的情况下，安全地在线程间传递状态信息，同时保证相关数据的可见性。

## 4、volatile 可以保证原子性么？

**`volatile` 关键字能保证变量的可见性，但不能保证对变量的操作是原子性的。**

我们通过下面的代码即可证明：

```java
public class VolatileAtomicityDemo {
    public volatile static int inc = 0;

    public void increase() {
        inc++;
    }

    public static void main(String[] args) throws InterruptedException {
        ExecutorService threadPool = Executors.newFixedThreadPool(5);
        VolatileAtomicityDemo volatileAtomicityDemo = new VolatileAtomicityDemo();
        for (int i = 0; i < 5; i++) {
            threadPool.execute(() -> {
                for (int j = 0; j < 500; j++) {
                    volatileAtomicityDemo.increase();
                }
            });
        }
        // 等待1.5秒，保证上面程序执行完成
        Thread.sleep(1500);
        System.out.println(inc);
        threadPool.shutdown();
    }
}
```

正常情况下，运行上面的代码理应输出 `2500`。但你真正运行了上面的代码之后，你会发现每次输出结果都小于 `2500`。

为什么会出现这种情况呢？不是说好了，`volatile` 可以保证变量的可见性嘛！

也就是说，如果 `volatile` 能保证 `inc++` 操作的原子性的话。每个线程中对 `inc` 变量自增完之后，其他线程可以立即看到修改后的值。5 个线程分别进行了 500 次操作，那么最终 inc 的值应该是 5*500=2500。

很多人会误认为自增操作 `inc++` 是原子性的，实际上，`inc++` 其实是一个复合操作，包括三步：

1. 读取 inc 的值。
2. 对 inc 加 1。
3. 将 inc 的值写回内存。

`volatile` 是无法保证这三个操作是具有原子性的，有可能导致下面这种情况出现：

1. 线程 1 对 `inc` 进行读取操作之后，还未对其进行修改。线程 2 又读取了 `inc`的值并对其进行修改（+1），再将`inc` 的值写回内存。
2. 线程 2 操作完毕后，线程 1 对 `inc`的值进行修改（+1），再将`inc` 的值写回内存。

这也就导致两个线程分别对 `inc` 进行了一次自增操作后，`inc` 实际上只增加了 1。

其实，如果想要保证上面的代码运行正确也非常简单，利用 `synchronized`、`Lock`或者`AtomicInteger`都可以。

使用 `synchronized` 改进：

```java
public synchronized void increase() {
    inc++;
}
```

使用 `AtomicInteger` 改进：

```java
public AtomicInteger inc = new AtomicInteger();

public void increase() {
    inc.getAndIncrement();
}
```

使用 `ReentrantLock` 改进：

```java
Lock lock = new ReentrantLock();
public void increase() {
    lock.lock();
    try {
        inc++;
    } finally {
        lock.unlock();
    }
}
```



# ⭐️三、乐观锁和悲观锁

## 1、什么是悲观锁？

悲观锁总是假设最坏的情况，认为共享资源每次被访问的时候就会出现问题(比如共享数据被修改)，所以每次在获取资源操作的时候都会上锁，这样其他线程想拿到这个资源就会阻塞直到锁被上一个持有者释放。也就是说，**共享资源每次只给一个线程使用，其它线程阻塞，用完后再把资源转让给其它线程**。

像 Java 中`synchronized`和`ReentrantLock`等独占锁就是悲观锁思想的实现。

```java
public void performSynchronisedTask() {
    synchronized (this) {
        // 需要同步的操作
    }
}

private Lock lock = new ReentrantLock();
lock.lock();
try {
   // 需要同步的操作
} finally {
    lock.unlock();
}
```

高并发的场景下，激烈的锁竞争会造成线程阻塞，大量阻塞线程会导致系统的上下文切换，增加系统的性能开销。并且，悲观锁还可能会存在死锁问题，影响代码的正常运行。

> 👉 **频繁上下文切换，并不是因为“抢锁失败”本身**
>  👉 而是因为：线程在“阻塞 ↔ 就绪 ↔ 运行”之间反复切换
>
> ## 一、关键过程拆解（重点）
>
> 1️⃣ 锁释放
>
> ```java
> 线程A 释放锁
> ```
>
> 2️⃣ 操作系统 / JVM 唤醒等待线程
>
> ```
> 线程B、C、D 被唤醒 → 进入 RUNNABLE（就绪态）
> ```
>
> ⚠️ 注意：
>
> 👉 **不是只唤醒一个，而是可能唤醒多个（甚至全部）**
>
> 3️⃣ CPU 调度开始（重点来了）
>
> CPU 不知道谁能抢到锁，它只能：
>
> ```
> 调度 B → 运行
> 调度 C → 运行
> 调度 D → 运行
> ```
>
> 👉 **这一步就已经产生上下文切换了！**
>
> ## 二、为什么会频繁切换？（核心原因）
>
> ### 🔥 原因1：线程被唤醒后必须“试一下”
>
> 线程被唤醒后：
>
> - 不会直接知道自己拿不到锁
> - 必须**真正运行一下，尝试获取锁**
>
> ```
> B 被调度 → 尝试抢锁 → 失败 → 再阻塞
> ```
>
> 👉 这个过程是：
>
> ```
> 阻塞 → 就绪 → 运行 → 阻塞
> ```
>
> ⚠️ 每一步都可能触发上下文切换！
>
> ### 🔥 原因2：操作系统是“时间片轮转”
>
> CPU 调度不是只跑一个线程：
>
> ```
> B 运行一会儿 → 时间片用完 → 切到 C
> C 再运行 → 切到 D
> ```
>
> 👉 即使他们都抢不到锁，也会被调度执行
>
> ------
>
> ### 🔥 原因3：抢锁失败会再次进入阻塞
>
> ```
> B 抢锁失败 → park / 阻塞
> ```
>
> 👉 又发生一次状态切换：
>
> ```
> RUNNING → BLOCKED
> ```
>
> ## 四、完整链路（你要掌握这个）
>
> 一次“无效竞争”实际发生了什么：
>
> ```
> 1. B 被唤醒（BLOCKED → RUNNABLE）   ✅ 切换
> 2. CPU 调度 B（RUNNABLE → RUNNING）  ✅ 切换
> 3. B 抢锁失败（RUNNING → BLOCKED）   ✅ 切换
> ```
>
> 👉 一个线程失败一次，就可能产生 **2~3 次上下文切换**

## 2、什么是乐观锁？

乐观锁总是假设最好的情况，认为共享资源每次被访问的时候不会出现问题，线程可以不停地执行，无需加锁也无需等待，只是在提交修改的时候去验证对应的资源（也就是数据）是否被其它线程修改了（具体方法可以使用版本号机制或 CAS 算法）。

在 Java 中`java.util.concurrent.atomic`包下面的原子变量类（比如`AtomicInteger`、`LongAdder`）就是使用了乐观锁的一种实现方式 **CAS** 实现的。

![](./assets/JUC%E5%8E%9F%E5%AD%90%E7%B1%BB%E6%A6%82%E8%A7%88-20230814005211968.png)

```java
// LongAdder 在高并发场景下会比 AtomicInteger 和 AtomicLong 的性能更好
// 代价就是会消耗更多的内存空间（空间换时间）
LongAdder sum = new LongAdder();
sum.increment();
```



