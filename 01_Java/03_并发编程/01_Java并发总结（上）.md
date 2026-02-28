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