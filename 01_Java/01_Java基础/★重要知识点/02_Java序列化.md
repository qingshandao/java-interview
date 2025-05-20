# 一、什么是序列化和反序列化?

如果我们需要持久化 Java 对象比如将 Java 对象保存在文件中，或者在网络传输 Java 对象，这些场景都需要用到序列化。

简单来说：

- **序列化**：将数据结构或对象转换成可以存储或传输的形式，通常是二进制字节流，也可以是 JSON, XML 等文本格式
- **反序列化**：将在序列化过程中所生成的数据转换为原始数据结构或者对象的过程

对于 Java 这种面向对象编程语言来说，我们序列化的都是对象（Object）也就是实例化后的类(Class)，但是在 C++这种半面向对象的语言中，struct(结构体)定义的是数据结构类型，而 class 对应的是对象类型。

下面是序列化和反序列化常见应用场景：

- 对象在进行网络传输（比如远程方法调用 RPC 的时候）之前需要先被序列化，接收到序列化的对象之后需要再进行反序列化；
- 将对象存储到文件之前需要进行序列化，将对象从文件中读取出来需要进行反序列化；
- 将对象存储到数据库（如 Redis）之前需要用到序列化，将对象从缓存数据库中读取出来需要反序列化；
- 将对象存储到内存之前需要进行序列化，从内存中读取出来之后需要进行反序列化。

维基百科是如是介绍序列化的：

> **序列化**（serialization）在计算机科学的数据处理中，是指将数据结构或对象状态转换成可取用格式（例如存成文件，存于缓冲，或经由网络中发送），以留待后续在相同或另一台计算机环境中，能恢复原先状态的过程。依照序列化格式重新获取字节的结果时，可以利用它来产生与原始对象相同语义的副本。对于许多对象，像是使用大量引用的复杂对象，这种序列化重建的过程并不容易。面向对象中的对象序列化，并不概括之前原始对象所关系的函数。这种过程也称为对象编组（marshalling）。从一系列字节提取数据结构的反向操作，是反序列化（也称为解编组、deserialization、unmarshalling）。

综上：**序列化的主要目的是通过网络传输对象或者说是将对象存储到文件系统、数据库、内存中。**

![](./../assets/serialization_interview.png)

## 1、序列化协议对应于 TCP/IP 4 层模型的哪一层？

我们知道网络通信的双方必须要采用和遵守相同的协议。TCP/IP 四层模型是下面这样的，序列化协议属于哪一层呢？

1. 应用层
2. 传输层
3. 网络层
4. 网络接口层

![](./../assets/tcp-ip-4-model.png)

如上图所示，OSI 七层协议模型中，表示层做的事情主要就是对应用层的用户数据进行处理转换为二进制流。反过来的话，就是将二进制流转换成应用层的用户数据。这不就对应的是序列化和反序列化么？

因为，OSI 七层协议模型中的应用层、表示层和会话层对应的都是 TCP/IP 四层模型中的应用层，所以序列化协议属于 TCP/IP 协议应用层的一部分。

# 二、常见序列化协议有哪些？

JDK 自带的序列化方式一般不会用 ，因为序列化效率低并且存在安全问题。比较常用的序列化协议有 Hessian、Kryo、Protobuf、ProtoStuff，这些都是基于二进制的序列化协议。

像 JSON 和 XML 这种属于文本类序列化方式。虽然可读性比较好，但是性能较差，一般不会选择。

## 1、JDK 自带的序列化方式

JDK 自带的序列化，只需实现 `java.io.Serializable`接口即可。

```java
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@ToString
public class RpcRequest implements Serializable {
    private static final long serialVersionUID = 1905122041950251207L;
    private String requestId;
    private String interfaceName;
    private String methodName;
    private Object[] parameters;
    private Class<?>[] paramTypes;
    private RpcMessageTypeEnum rpcMessageTypeEnum;
}
```

### （1）**serialVersionUID 有什么作用？**

序列化号 `serialVersionUID` 属于版本控制的作用。反序列化时，会检查 `serialVersionUID` 是否和当前类的 `serialVersionUID` 一致。如果 `serialVersionUID` 不一致则会抛出 `InvalidClassException` 异常。强烈推荐每个序列化类都手动指定其 `serialVersionUID`，如果不手动指定，那么编译器会动态生成默认的 `serialVersionUID`。

### （2）**serialVersionUID 不是被 static 变量修饰了吗？为什么还会被“序列化”？**

`static` 修饰的变量是静态变量，属于类而非类的实例，本身是不会被序列化的。然而，`serialVersionUID` 是一个特例，`serialVersionUID` 的序列化做了特殊处理。当一个对象被序列化时，`serialVersionUID` 会被写入到序列化的二进制流中；在反序列化时，也会解析它并做一致性判断，以此来验证序列化对象的版本一致性。如果两者不匹配，反序列化过程将抛出 `InvalidClassException`，因为这通常意味着序列化的类的定义已经发生了更改，可能不再兼容。

✅ 官方说明如下：

> A serializable class can declare its own serialVersionUID explicitly by declaring a field named `"serialVersionUID"` that must be `static`, `final`, and of type `long`;
>
> 如果想显式指定 `serialVersionUID` ，则需要在类中使用 `static` 和 `final` 关键字来修饰一个 `long` 类型的变量，变量名字必须为 `"serialVersionUID"` 。

⚠也就是说，`serialVersionUID` **只是用来被 JVM 识别，实际并没有被序列化**。

✅ 详解如下：

#### a、`serialVersionUID` 是 `static final` 变量，为什么还会“被序列化”？

答案是：它其实并不会被真正序列化进对象的字节流中。
但它会在序列化和反序列化过程中被 JVM 拿来“参与比对”，起到了校验的作用，这就让人有“它被序列化”的错觉。

#### b、`serialVersionUID` 的作用到底是什么？

在 Java 的序列化机制中：

- 序列化时，会把类的 serialVersionUID 写入序列化流头中（不是对象数据部分，而是对象的元信息）。

- 反序列化时，JVM 会拿反序列化目标类的 serialVersionUID 和流中记录的 serialVersionUID 做比对。
  - 如果一致，就继续反序列化。
  - 如果不一致，就抛出 InvalidClassException。

这保证了 “序列化前和反序列化时的类版本必须一致”。

#### c、那为什么它是 `static` 的还能起作用？

这是因为：

- serialVersionUID 是被 JVM 读取的，不是被序列化的。

- JVM 在读取序列化流的时候，会从类定义中（通过反射）获取 serialVersionUID 静态变量的值来进行校验。

所以它被声明为 static，不会随对象被序列化，但却是序列化机制的重要组成部分。

#### d、如果不手动写 `serialVersionUID` 会怎样？

如果你不写，JVM 会根据类的结构自动计算一个 UID，但：

- 这个自动计算机制比较敏感，类一旦结构变了（加个字段、改个方法签名），UID 可能就变了；

- 导致你旧数据反序列化新类的时候失败（`InvalidClassException`）；

- 所以推荐手动声明一个固定的 `serialVersionUID`。

### （3）**如果有些字段不想进行序列化怎么办？**

对于不想进行序列化的变量，可以使用 `transient` 关键字修饰。

`transient` 关键字的作用是：阻止实例中那些用此关键字修饰的的变量序列化；当对象被反序列化时，被 `transient` 修饰的变量值不会被持久化和恢复。

关于 `transient` 还有几点注意：

- `transient` 只能修饰变量，不能修饰类和方法。
- `transient` 修饰的变量，在反序列化后变量值将会被置成类型的默认值。例如，如果是修饰 `int` 类型，那么反序列后结果就是 `0`。
- `static` 变量因为不属于任何对象(Object)，所以无论有没有 `transient` 关键字修饰，均不会被序列化。

#### （a）为什么需要 `transient`？

Java 的对象默认是可序列化的（前提是类实现了 Serializable 接口）。但是：

> 有些字段我们不希望被序列化，这是 transient 出现的原因。

例如：

```java
private transient String password;
```

#### （b）什么情况下我们不希望某个字段被序列化？

以下是一些典型场景：

1. 敏感信息（如密码、身份证号）

   ```java
   private transient String password;
   ```

   原因：安全性。不希望密码被写进硬盘、日志或通过网络传输。

2. 非持久化计算字段（缓存/临时值）

   ```java
   private transient int cachedHashCode;
   ```

   原因：这类字段可以在运行时重新计算，没有必要持久化。

3. 与特定平台或上下文有关的资源

   ```java
   private transient Socket socket;
   private transient Thread thread;
   ```

   ⚠原因：这些对象无法被序列化（比如线程、文件句柄、数据库连接等），序列化它们没有意义，反而会抛异常。

4. 避免循环引用或大型对象图

   有时你只想序列化核心数据，而不希望序列化一整棵复杂对象图。使用 transient 可以“切断”这类引用。

5. 节省空间

   某些字段占内存大，但不影响业务逻辑（如日志、缓存等），可以用 transient 跳过，减少序列化后的数据体积。

### （4）**为什么不推荐使用 JDK 自带的序列化？**

我们很少或者说几乎不会直接使用 JDK 自带的序列化方式，主要原因有下面这些原因：

- **不支持跨语言调用** : 如果调用的是其他语言开发的服务的时候就不支持了。
- **性能差**：相比于其他序列化框架性能更低，主要原因是序列化之后的字节数组体积较大，导致传输成本加大。
- **存在安全问题**：序列化和反序列化本身并不存在问题。但当输入的反序列化的数据可被用户控制，那么攻击者即可通过构造恶意输入，让反序列化产生非预期的对象，在此过程中执行构造的任意代码。相关阅读：[应用安全:JAVA 反序列化漏洞之殇 - Cryin](https://cryin.github.io/blog/secure-development-java-deserialization-vulnerability/)、[Java 反序列化安全漏洞怎么回事? - Monica](https://www.zhihu.com/question/37562657/answer/1916596031)。

