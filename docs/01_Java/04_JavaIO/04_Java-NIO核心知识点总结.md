# 一、NIO 简介

在传统的 Java I/O 模型（BIO）中，I/O 操作是以阻塞的方式进行的。也就是说，当一个线程执行一个 I/O 操作时，它会被阻塞直到操作完成。这种阻塞模型在处理多个并发连接时可能会导致性能瓶颈，因为需要为每个连接创建一个线程，而线程的创建和切换都是有开销的。

为了解决这个问题，在 Java1.4 版本引入了一种新的 I/O 模型 — **NIO** （New IO，也称为 Non-blocking IO） 。NIO 弥补了同步阻塞 I/O 的不足，它在标准 Java 代码中提供了非阻塞、面向缓冲、基于通道的 I/O，可以使用少量的线程来处理多个连接，大大提高了 I/O 效率和并发。

下图是 BIO、NIO 和 AIO 处理客户端请求的简单对比图（关于 AIO 的介绍，可以看我写的这篇文章：[Java IO 模型详解](./03_Java-IO模型详解.md)，不是重点，了解即可）。

![](./assets/bio-aio-nio.png)

⚠️需要注意：使用 NIO 并不一定意味着高性能，它的性能优势主要体现在高并发和高延迟的网络环境下。当连接数较少、并发程度较低或者网络传输速度较快时，NIO 的性能并不一定优于传统的 BIO 。

# 二、NIO 核心组件

NIO 主要包括以下三个核心组件：

- **Buffer（缓冲区）**：NIO 读写数据都是通过缓冲区进行操作的。**读**操作的时候将 Channel 中的数据填充到 Buffer 中，而**写**操作时将 Buffer 中的数据写入到 Channel 中。
- **Channel（通道）**：Channel 是一个双向的、可读可写的数据传输通道，NIO 通过 Channel 来实现数据的输入输出。通道是一个抽象的概念，它可以代表文件、套接字或者其他数据源之间的连接。
- **Selector（选择器）**：允许一个线程处理多个 Channel，基于事件驱动的 I/O 多路复用模型。所有的 Channel 都可以注册到 Selector 上，由 Selector 来分配线程来处理事件。

三者的关系如下图所示（暂时不理解没关系，后文会详细介绍）：

![](./assets/channel-buffer-selector.png)

下面详细介绍一下这三个组件。

## 1、Buffer（缓冲区）

在传统的 BIO 中，数据的读写是面向流的， 分为字节流和字符流。

在 Java 1.4 的 NIO 库中，所有数据都是用**缓冲区**处理的，这是新库和之前的 BIO 的一个重要区别，有点类似于 BIO 中的缓冲流。NIO 在读取数据时，它是直接读到缓冲区中的。在写入数据时，写入到缓冲区中。 使用 NIO 在读写数据时，都是通过缓冲区进行操作。

`Buffer` 的子类如下图所示。其中，最常用的是 `ByteBuffer`，它可以用来存储和操作字节数据。

![](./assets/buffer-subclasses.png)

你可以将 Buffer 理解为一个数组，`IntBuffer`、`FloatBuffer`、`CharBuffer` 等分别对应 `int[]`、`float[]`、`char[]` 等。

为了更清晰地认识缓冲区，我们来简单看看`Buffer` 类中定义的四个成员变量：

```java
public abstract class Buffer {
    // Invariants: mark <= position <= limit <= capacity
    
    private int mark = -1;
    private int position = 0;
    private int limit;
    private int capacity;
}
```

这四个成员变量的具体含义如下：

1. 容量（`capacity`）：`Buffer`可以存储的最大数据量，`Buffer`创建时设置且不可改变；
2. 界限（`limit`）：`Buffer` 中可以读/写数据的边界。写模式下，`limit` 代表最多能写入的数据，一般等于 `capacity`（可以通过`limit(int newLimit)`方法设置）；读模式下，`limit` 等于 Buffer 中实际写入的数据大小。
3. 位置（`position`）：**下一个**可以被读写的数据的**位置（索引）**。从写操作模式到读操作模式**切换的时候**（flip），**`position` 都会归零**，这样就可以从头开始读写了。
4. 标记（`mark`）：`Buffer`允许将位置直接定位到该标记处，这是一个可选属性；

并且，上述变量满足如下的关系：**0 <= mark <= position <= limit <= capacity** 。

另外，Buffer 有读模式和写模式这两种模式，分别用于从 Buffer 中读取数据或者向 Buffer 中写入数据。Buffer 被创建之后**默认是写模式**，调用 `flip()` 可以切换到读模式。如果要再次切换回写模式，可以调用 `clear()` 或者 `compact()` 方法。



### 1.1、capacity（容量）的单位是什么？

capacity 的单位不是“字节固定值”，而是：当前 Buffer 所能存储的“**元素个数**”，不同 Buffer 类型，单位不同。

#### ByteBuffer

```
ByteBuffer buffer = ByteBuffer.allocate(1024);
```

这里：`capacity = 1024`，表示：可存储 1024 个 byte。由于 1 byte = 1 字节，因此这里也等于：1024 字节。

#### IntBuffer

```
IntBuffer buffer = IntBuffer.allocate(10);
```

这里：capacity = 10，表示：能存 10 个 int

而不是 10 字节，因为：1 int = 4 字节，因此实际占用：40 字节

#### 总结

| Buffer类型 | capacity含义 |
| ---------- | ------------ |
| ByteBuffer | byte个数     |
| CharBuffer | char个数     |
| IntBuffer  | int个数      |
| LongBuffer | long个数     |

所以：capacity 的单位 = 当前 Buffer 元素类型的个数，不是固定“字节”。

### 1.2、 position 的值是什么？

position 的本质表示：**下一个**可读/可写元素的位置索引。

⚠注意：不是当前元素，而是“下一次操作”的位置。

------

#### 写模式下

初始：

```
ByteBuffer buffer = ByteBuffer.allocate(10);
```

状态：

```
position = 0
limit = 10
capacity = 10
```

------

 `put()` 后 position 自动增加

```
buffer.put((byte)1);
```

发生：数据写入索引0，然后：`position -> 1`，表示：下一个写入位置是1。

继续：

```
buffer.put((byte)2);
```

状态：`position = 2`，因为：0、1 已经写过，下一个写入位置是2

------

#### 读模式下

调用`flip();`后：

```
limit = position原值
position = 0
```

例如：原来写了2个字节，`flip` 后：

```
limit = 2
position = 0
```

表示：只能读取 0~1。

#### get() 后 position 也会增加

```
buffer.get();
```

读取索引0后，

```
position = 1
```

再读一次：

```
position = 2
```

此时：

```
position == limit
```

说明：

```
数据读完了
```

------

### 1.3、position 如何修改？

有三种方式：

#### （1）put/get 自动修改

最常见。

------

#### (2）flip()/clear()/compact() 修改

**flip()**

```
position = 0
```

 **clear()**

```
position = 0
limit = capacity
```

------

### compact()

会把未读数据前移：

例如：

```
[1 2 3 4]
```

已经读了：

```
1 2
```

剩余：

```
3 4
```

compact 后：

```
3 4 _ _
```

然后：

```
position = 2
```

因为：

```
前面已有2个有效数据
下次从索引2继续写
```

------

## 3）手动修改

```
buffer.position(5);
```

直接设置 position。

类似文件指针跳转。

