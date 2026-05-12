# 一、AQS 介绍

AQS 的全称为 `AbstractQueuedSynchronizer` ，翻译过来的意思就是抽象队列同步器。这个类在 `java.util.concurrent.locks` 包下面。

![](./../assets/AQS.png)

AQS 就是一个抽象类，主要用来构建锁和同步器。

```java
public abstract class AbstractQueuedSynchronizer extends AbstractOwnableSynchronizer implements java.io.Serializable {
}
```

AQS 为构建锁和同步器提供了一些通用功能的实现。因此，使用 AQS 能简单且高效地构造出应用广泛的大量的同步器，比如我们提到的 `ReentrantLock`，`Semaphore`，其他的诸如 `ReentrantReadWriteLock`，`SynchronousQueue`等等皆是基于 AQS 的。

# 二、AQS 原理

在面试中被问到并发知识的时候，大多都会被问到“请你说一下自己对于 AQS 原理的理解”。下面给大家一个示例供大家参考，面试不是背题，大家一定要加入自己的思想，即使加入不了自己的思想也要保证自己能够通俗的讲出来而不是背出来。

## 1、AQS 快速了解

在真正讲解 AQS 源码之前，需要对 AQS 有一个整体层面的认识。这里会先通过几个问题，从整体层面上认识 AQS，了解 AQS 在整个 Java 并发中所位于的层面，之后在学习 AQS 源码的过程中，才能更加了解同步器和 AQS 之间的关系。

### 1.1、AQS 的作用是什么？

AQS 解决了开发者在实现同步器时的复杂性问题。它提供了一个通用框架，用于实现各种同步器，例如 **可重入锁**（`ReentrantLock`）、**信号量**（`Semaphore`）和 **倒计时器**（`CountDownLatch`）。通过封装底层的线程同步机制，AQS 将复杂的线程管理逻辑隐藏起来，使开发者只需专注于具体的同步逻辑。

简单来说，AQS 是一个抽象类，为同步器提供了通用的 **执行框架**。它定义了 **资源获取和释放的通用流程**，而具体的资源获取逻辑则由具体同步器通过重写模板方法来实现。 因此，可以将 AQS 看作是同步器的 **基础“底座”**，而同步器则是基于 AQS 实现的 **具体“应用”**。

### 1.2、AQS 为什么使用 CLH 锁队列的变体？

CLH 锁是一种基于 **自旋锁** 的优化实现。

先说一下自旋锁存在的问题：自旋锁通过线程不断对一个原子变量执行 `compareAndSet`（简称 `CAS`）操作来尝试获取锁。在高并发场景下，多个线程会同时竞争同一个原子变量，容易造成某个线程的 `CAS` 操作长时间失败，从而导致 **“饥饿”问题**（某些线程可能永远无法获取锁）。

> AQS中的 `compareAndSet` 方法并没有自己实现原子操作，而是依赖于 `sun.misc.Unsafe` 类。Unsafe提供的CAS方法是 `compareAndSwap` 前缀的，这是一个与特定CPU指令集（如x86架构下的`CMPXCHG`）直接对应的底层操作
>
> 比如，在OpenJDK源码中，`compareAndSetState` 方法的典型实现如下：
>
> ```java
> protected final boolean compareAndSetState(int expect, int update) {
>     return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
> }
> ```
>
> 你可能会困惑，为什么会有 `compareAndSwap` 和 `compareAndSet` 这两种叫法？其实就像 `JVM` 和 `JDK` 的关系一样，`compareAndSwap` 是偏向底层的硬件术语，更能反映它"比较并交换"的机制；而 `compareAndSet` 则是面向Java应用的API命名，语义更清晰，因此在`AtomicInteger`的API设计中也采用了 `compareAndSet()` 来命名。

CLH 锁通过引入一个队列来组织并发竞争的线程，对自旋锁进行了改进：

- 每个线程会作为一个节点加入到队列中，并通过自旋监控前一个线程节点的状态，而不是直接竞争共享变量。
- 线程按顺序排队，确保公平性，从而避免了 “饥饿” 问题。

AQS（AbstractQueuedSynchronizer）在 CLH 锁的基础上进一步优化，形成了其内部的 **CLH 队列变体**。主要改进点有以下两方面：

1. **自旋 + 阻塞**： 

   CLH 锁使用纯自旋方式等待锁的释放，但大量的自旋操作会占用过多的 CPU 资源。AQS 引入了 自旋 + 阻塞 的混合机制： 

   - 如果线程获取锁失败，会先短暂自旋尝试获取锁；
   - 如果仍然失败，则线程会进入阻塞状态，等待被唤醒，从而减少 CPU 的浪费。

2. **单向队列改为双向队列**：CLH 锁使用单向队列，节点只知道前驱节点的状态，而当某个节点释放锁时，需要通过队列唤醒后续节点。AQS 将队列改为 **双向队列**，新增了 `next` 指针，使得节点不仅知道前驱节点，也可以直接唤醒后继节点，从而简化了队列操作，提高了唤醒效率。

### 1.3、AQS 的性能比较好，原因是什么？

因为 AQS 内部大量使用了 `CAS` 操作。

AQS 内部通过队列来存储等待的线程节点。由于**队列是共享资源**，在多线程场景下，需要保证队列的同步访问。

AQS 内部通过 `CAS` 操作来控制队列的同步访问，`CAS` 操作主要用于控制 `队列初始化` 、 `线程节点入队` 两个操作的并发安全。虽然利用 `CAS` 控制并发安全可以保证比较好的性能，但同时会带来比较高的 **编码复杂度** 。

### 1.4、AQS 中为什么 Node 节点需要不同的状态？

AQS 中的 `waitStatus` 状态类似于 **状态机** ，通过不同状态来表明 Node 节点的不同含义，并且根据不同操作，来控制状态之间的流转。

可以把 AQS 的 `Node.waitStatus` 理解成：“**前驱节点对后继节点的承诺状态**”，它本质上不是“当前节点自己的状态”，而是：“当前节点在释放锁时，需不需要去唤醒后继节点”。

在 AQS 中，每个等待获取锁的线程，都会被包装成一个 `Node` 节点，加入到双向链表队列（CLH 队列）中，AQS 队列结构：

```
head <-> node1 <-> node2 <-> node3
```

每个 Node 中主要存储：

```java
static final class Node {
    volatile int waitStatus;

    volatile Node prev;
    volatile Node next;

    volatile Thread thread;

    Node nextWaiter;
}
```

核心字段：

| 字段       | 作用               |
| ---------- | ------------------ |
| thread     | 当前节点对应的线程 |
| prev       | 前驱节点           |
| next       | 后继节点           |
| waitStatus | 节点状态           |
| nextWaiter | Condition队列使用  |



- 状态 `0` ：新节点加入队列之后，初始状态为 `0` 。
- 状态 `SIGNAL(-1)` ：当有新的节点加入队列，此时新节点的前继节点状态就会由 `0` 更新为 `SIGNAL` ，表明前继节点释放锁之后，需要对后继节点进行唤醒操作。如果唤醒 `SIGNAL` 状态节点的后续节点，当前节点就会将 `SIGNAL` 状态更新为 `0` 。即通过清除 `SIGNAL` 状态，表示已经执行了唤醒操作。因此SIGNAL 是“一次性通知状态”，不是永久状态。
- 状态 `CANCELLED` ：如果一个节点在队列中等待获取锁锁时，因为某种原因失败了，该节点的状态就会变为 `CANCELLED` ，表明取消获取锁，这种状态的节点是异常的，无法被唤醒，也无法唤醒后继节点。

