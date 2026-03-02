# 一、线程

## 1、⭐️什么是线程和进程?

### 1.1、何为进程?

进程是程序的一次执行过程，是系统运行程序的基本单位，因此进程是动态的。系统运行一个程序即是一个进程从创建，运行到消亡的过程。

在 Java 中，当我们启动 main 函数时其实就是启动了一个 JVM 的进程，而 main 函数所在的线程就是这个进程中的一个线程，也称主线程。

在 Windows 中通过查看任务管理器的方式，我们就可以清楚看到 Windows 当前运行的进程（`.exe` 文件的运行）。

### 1.2、何为线程?

线程与进程相似，但线程是一个比进程更小的执行单位。一个进程在其执行的过程中可以产生多个线程。与进程不同的是，同类的多个线程共享进程的**堆**和**方法区**资源，但每个线程有自己的**程序计数器**、**虚拟机栈**和**本地方法栈**，所以系统在产生一个线程，或是在各个线程之间做切换工作时，负担要比进程小得多，也正因为如此，线程也被称为轻量级进程。

> 注意：
>
> - 同类的多个线程：通过同一个Thread子类创建的多个实例，或者执行相同Runnable/Callable任务的多个线程
>
>   ```java
>   /**
>    * Java线程内存模型详解
>    * 
>    * 1. 同类的多个线程：
>    *    - 通过同一个Thread子类创建的多个实例
>    *    - 或者执行相同Runnable/Callable任务的多个线程
>    *    - 例如：new MyThread(), new MyThread() 或者 Thread t1=new Thread(r), Thread t2=new Thread(r)
>    * 
>    * 2. 共享资源（堆和方法区）：
>    *    堆（Heap）：存放对象实例、数组等，所有线程共享
>    *    方法区（Method Area）：存放类信息、常量、静态变量、JIT编译后的代码等
>    * 
>    * 3. 线程私有资源：
>    *    程序计数器（PC Register）：记录当前线程执行的字节码指令位置
>    *    虚拟机栈（VM Stack）：存储局部变量表、操作数栈、动态链接等
>    *    本地方法栈（Native Method Stack）：为本地方法服务
>    */
>   
>   public class ThreadMemoryModelDemo {
>       // 方法区资源：类的静态变量
>       private static int staticCounter = 5;
>       
>       // 堆资源：实例变量（对象的一部分）
>       private String objectName;
>       
>       public ThreadMemoryModelDemo(String name) {
>           this.objectName = name;
>       }
>       
>       // 方法区资源：方法字节码
>       public void sharedMethod() {
>           // 以下变量存储在线程私有的虚拟机栈中
>           int localVar = 10;           // 局部变量
>           String localRef = "test";    // 局部引用
>           
>           // 访问共享的堆资源
>           System.out.println(this.objectName);
>           
>           // 访问共享的方法区资源
>           System.out.println("Static counter: " + staticCounter);
>       }
>       
>       // 演示多线程共享资源
>       public static void demonstrateSharedResource() {
>           ThreadMemoryModelDemo demo1 = new ThreadMemoryModelDemo("Thread-1");
>           ThreadMemoryModelDemo demo2 = new ThreadMemoryModelDemo("Thread-2");
>           
>           // 创建多个线程执行相同任务
>           Runnable task = () -> {
>               // 每个线程有自己的程序计数器，记录执行位置
>               // 每个线程有自己的虚拟机栈，localVar存储在这里
>               
>               // 但访问相同的共享资源
>               demo1.sharedMethod();  // 访问堆中的对象
>               System.out.println("Static value accessed by thread: " + staticCounter); // 方法区资源
>           };
>           
>           Thread t1 = new Thread(task, "Thread-1");
>           Thread t2 = new Thread(task, "Thread-2");
>           
>           t1.start();
>           t2.start();
>       }
>       
>       public static void main(String[] args) {
>           System.out.println("=== Java线程内存模型说明 ===\n");
>           
>           System.out.println("1. 同类的多个线程：");
>           System.out.println("   - 通过相同方式创建的多个线程实例");
>           System.out.println("   - 例如：多个Thread实例执行相同任务\n");
>           
>           System.out.println("2. 共享资源：");
>           System.out.println("   - 堆（Heap）：存储所有对象实例，所有线程共享");
>           System.out.println("   - 方法区（Method Area）：存储类元数据、静态变量、常量池等\n");
>           
>           System.out.println("3. 线程私有资源：");
>           System.out.println("   - 程序计数器：记录线程执行位置，线程切换时保持状态");
>           System.out.println("   - 虚拟机栈：存储方法调用的局部变量和操作栈");
>           System.out.println("   - 本地方法栈：支持native方法执行\n");
>           
>           demonstrateSharedResource();
>       }
>   }
>   ```
>
>   

Java 程序天生就是多线程程序，我们可以通过 JMX 来看看一个普通的 Java 程序有哪些线程，代码如下。

```java
public class MultiThread {
	public static void main(String[] args) {
		// 获取 Java 线程管理 MXBean
	ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		// 不需要获取同步的 monitor 和 synchronizer 信息，仅获取线程和线程堆栈信息
		ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(false, false);
		// 遍历线程信息，仅打印线程 ID 和线程名称信息
		for (ThreadInfo threadInfo : threadInfos) {
			System.out.println("[" + threadInfo.getThreadId() + "] " + threadInfo.getThreadName());
		}
	}
}
```

上述程序输出如下（输出内容可能不同，不用太纠结下面每个线程的作用，只用知道 main 线程执行 main 方法即可）：

```java
[5] Attach Listener //添加事件
[4] Signal Dispatcher // 分发处理给 JVM 信号的线程
[3] Finalizer //调用对象 finalize 方法的线程
[2] Reference Handler //清除 reference 线程
[1] main //main 线程,程序入口
```

从上面的输出内容可以看出：**一个 Java 程序的运行是 main 线程和多个其他线程同时运行**。



## 2、Java 线程和操作系统的线程有啥区别？

JDK 1.2 之前，Java 线程是基于绿色线程（Green Threads）实现的，这是一种用户级线程（用户线程），也就是说 JVM 自己模拟了多线程的运行，而不依赖于操作系统。由于绿色线程和原生线程比起来在使用时有一些限制（比如绿色线程不能直接使用操作系统提供的功能如异步 I/O、只能在一个内核线程上运行，无法利用多核），在 JDK 1.2 及以后，Java 线程改为基于原生线程（Native Threads）实现，也就是说 JVM 直接使用操作系统原生的内核级线程（内核线程）来实现 Java 线程，由操作系统内核进行线程的调度和管理。

我们上面提到了用户线程和内核线程，考虑到很多读者不太了解二者的区别，这里简单介绍一下：

- 用户线程：由用户空间程序管理和调度的线程，运行在用户空间（专门给应用程序使用）。
- 内核线程：由操作系统内核管理和调度的线程，运行在内核空间（只有内核程序可以访问）。

顺便简单总结一下用户线程和内核线程的区别和特点：

- 用户线程创建和切换成本低，但不可以利用多核。
- 内核态线程，创建和切换成本高，可以利用多核。

一句话概括 Java 线程和操作系统线程的关系：**现在的 Java 线程的本质其实就是操作系统的线程**。

线程模型是用户线程和内核线程之间的关联方式，常见的线程模型有这三种：

1. 一对一（一个用户线程对应一个内核线程）
2. 多对一（多个用户线程映射到一个内核线程）
3. 多对多（多个用户线程映射到多个内核线程）

![](./assets/three-types-of-thread-models.png)

在 Windows 和 Linux 等主流操作系统中，Java 线程采用的是一对一的线程模型，也就是一个 Java 线程对应一个系统内核线程。Solaris 系统是一个特例（Solaris 系统本身就支持多对多的线程模型），HotSpot VM 在 Solaris 上支持多对多和一对一。具体可以参考 R 大的回答：[JVM中线程模型是用户级的吗](./References\JVM中线程模型是用户级的吗\JVM中的线程模型是用户级的么？ - 知乎.mhtml)

## 3、⭐️请简要描述线程与进程的关系,区别及优缺点？

下图是 Java 内存区域，通过下图我们从 JVM 的角度来说一下线程和进程之间的关系。

![](./assets/java-runtime-data-areas-jdk1.8.png)

从上图可以看出：一个进程中可以有多个线程，多个线程共享进程的**堆**和**方法区 (JDK1.8 之后的元空间)** 资源，但是每个线程有自己的 **程序计数器**、**虚拟机栈 和 ** **本地方法栈**。

⭐️**总结：** 

- 线程是进程划分成的更小的运行单位。
- 线程和进程最大的不同在于基本上各进程是独立的，而各线程则**不一定**，因为同一进程中的线程极有可能会相互影响。
- 线程执行开销小，但不利于资源的管理和保护；而进程正相反。

下面是该知识点的扩展内容！

下面来思考这样一个问题：为什么**程序计数器**、**虚拟机栈**和**本地方法栈**是线程私有的呢？为什么堆和方法区是线程共享的呢？

### 3.1、程序计数器为什么是私有的?

私有的程序计数器主要有下面两个作用：

1. 字节码解释器通过改变程序计数器来依次读取指令，从而实现代码的流程控制，如：顺序执行、选择、循环、异常处理。
2. 在多线程的情况下，程序计数器用于记录当前线程执行的位置，从而当线程被切换回来的时候能够知道该线程上次运行到哪儿了。

需要注意的是，如果执行的是 `native` 方法，那么程序计数器记录的是 `undefined`地址，只有执行的是 Java 代码时，程序计数器记录的才是下一条指令的地址，而执行其它语言代码时，程序计数器无法记录执行位置。

> 当执行native方法时，程序计数器记录`undefined`，因为：
>
> - native方法不在JVM的字节码执行体系中；
> - 执行的是本地机器代码；
> - JVM无法跟踪本地代码的执行位置；

所以，程序计数器私有主要是为了**线程切换后能恢复到正确的执行位置**，如果JVM只有一个全局程序计数器，所有线程共用，那么线程切换会导致**执行流交叉污染**，程序行为完全不可预测，甚至崩溃。

### 3.2、虚拟机栈和本地方法栈为什么是私有的?

- **虚拟机栈：** 每个 Java 方法在执行之前，会创建一个栈帧用于存储局部变量表、操作数栈、常量池引用等信息。从方法调用直至执行完成的过程，就对应着一个栈帧在 Java 虚拟机栈中入栈和出栈的过程。
- **本地方法栈：** 和虚拟机栈所发挥的作用非常相似，区别是：**虚拟机栈为虚拟机执行 Java 方法 （也就是字节码）服务，而本地方法栈则为虚拟机使用到的 Native 方法服务。** 在 HotSpot 虚拟机中和 Java 虚拟机栈合二为一。

所以，为了**保证线程中的局部变量不被别的线程访问到**，虚拟机栈和本地方法栈是线程私有的。

### 3.3、一句话简单了解堆和方法区

堆和方法区是 **所有线程共享** 的资源，其中堆是进程中最大的一块内存，主要用于存放新创建的对象 (几乎所有对象都在这里分配内存)，方法区主要用于存放已被加载的类信息、常量、静态变量、即时编译器编译后的代码等数据。

## 4、如何创建线程？

一般来说，创建线程有很多种方式，例如继承`Thread`类、实现`Runnable`接口、实现`Callable`接口、使用线程池、使用`CompletableFuture`类等等。

> 1. 继承 `Thread` 类：
>
>    ```java
>    class MyThread extends Thread {
>        @Override
>        public void run() {
>            System.out.println("线程执行：" + Thread.currentThread().getName());
>        }
>    }
>    
>    public class ThreadExample {
>        public static void main(String[] args) {
>            MyThread thread = new MyThread();
>            thread.start(); // 启动线程
>        }
>    }
>    ```
>
> 2. 实现 `Runnable` 接口：
>
>    这种方式更加灵活，因为Java支持单继承多实现：
>
>    ```java
>    class MyRunnable implements Runnable {
>        @Override
>        public void run() {
>            System.out.println("Runnable线程执行：" + Thread.currentThread().getName());
>        }
>    }
>    
>    public class RunnableExample {
>        public static void main(String[] args) {
>            MyRunnable runnable = new MyRunnable();
>            Thread thread = new Thread(runnable);
>            thread.start();
>        }
>    }
>    ```
>
> 3. 实现Callable接口
>
>    这种方式可以返回结果，并抛出异常：
>
>    ```java
>    import java.util.concurrent.Callable;
>    import java.util.concurrent.FutureTask;
>    
>    class MyCallable implements Callable<String> {
>        @Override
>        public String call() throws Exception {
>            Thread.sleep(1000); // 模拟耗时操作
>            return "执行完成，线程：" + Thread.currentThread().getName();
>        }
>    }
>    
>    public class CallableExample {
>        public static void main(String[] args) throws Exception {
>            MyCallable callable = new MyCallable();
>            FutureTask<String> futureTask = new FutureTask<>(callable);
>            Thread thread = new Thread(futureTask);
>            thread.start();
>            
>            String result = futureTask.get(); // 获取返回值
>            System.out.println(result);
>        }
>    }
>    ```
>
>    

不过，这些方式其实并没有真正创建出线程。准确点来说，这些都属于是在 Java 代码中使用多线程的方法。

严格来说，Java 就只有一种方式可以创建线程，那就是通过`new Thread().start()`创建。不管是哪种方式，最终还是依赖于`new Thread().start()`