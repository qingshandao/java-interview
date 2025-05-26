# 一、反射

## 1、何谓反射？

如果说大家研究过框架的底层原理或者咱们自己写过框架的话，一定对反射这个概念不陌生。

反射之所以被称为框架的灵魂，主要是因为它赋予了我们在运行时分析类以及执行类中方法的能力。

通过反射你可以获取任意一个类的所有属性和方法，你还可以调用这些方法和属性。

## 2、反射的优缺点？

- **优点：**反射可以让我们的代码更加灵活、为各种框架提供开箱即用的功能提供了便利。

- **缺点：**反射让我们在运行时有了分析操作类的能力的同时，也增加了安全问题。比如可以无视泛型参数的安全检查（泛型参数的安全检查发生在编译时）。另外，反射的性能也要稍差点，不过，对于框架来说实际是影响不大的。

相关阅读：[Java Reflection: Why is it so slow?](https://stackoverflow.com/questions/1392351/java-reflection-why-is-it-so-slow) 。

## 3、反射的应用场景？

像咱们平时大部分时候都是在写业务代码，很少会接触到直接使用反射机制的场景。

但是！这并不代表反射没有用。相反，正是因为反射，你才能这么轻松地使用各种框架。像 Spring/Spring Boot、MyBatis 等等框架中都大量使用了反射机制。

**这些框架中也大量使用了动态代理，而动态代理的实现也依赖反射。**

比如下面是通过 JDK 实现动态代理的示例代码，其中就使用了反射类 `Method` 来调用指定的方法。

```java
public class DebugInvocationHandler implements InvocationHandler {
    /**
     * 代理类中的真实对象
     */
    private final Object target;

    public DebugInvocationHandler(Object target) {
        this.target = target;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        System.out.println("before method " + method.getName());
        Object result = method.invoke(target, args);
        System.out.println("after method " + method.getName());
        return result;
    }
}
```

另外，像 Java 中的一大利器 **注解** 的实现也用到了反射。

为什么你使用 Spring 的时候 ，一个`@Component`注解就声明了一个类为 Spring Bean 呢？为什么你通过一个 `@Value`注解就读取到配置文件中的值呢？究竟是怎么起作用的呢？

这些都是因为你可以基于反射分析类，然后获取到类/属性/方法/方法的参数上的注解。你获取到注解之后，就可以做进一步的处理

# 二、实战

## 1、获取 Class 对象的四种方式

如果动态获取到这些信息，我们需要依靠 Class 对象。Class 类对象将一个类的方法、变量等信息告诉运行的程序。Java 提供了四种方式获取 Class 对象:

### （1）**知道具体类的情况下可以使用：**

```java
Class alunbarClass = TargetObject.class;
```

但是一般是不知道具体类的，基本都是通过遍历包下面的类来获取 Class 对象，通过此方式获取 Class 对象 **不会进行初始化**。

### （2） **通过 `Class.forName()`传入类的  全路径  获取：**

```java
Class alunbarClass1 = Class.forName("com.gc.TargetObject");
```

### （3）**通过对象实例`instance.getClass()`获取：**

```java
TargetObject o = new TargetObject();
Class alunbarClass2 = o.getClass();
```

### （4）**通过类加载器`xxxClassLoader.loadClass()`传入类路径获取:**

```java
ClassLoader.getSystemClassLoader().loadClass("cn.javaguide.TargetObject");
```



> - 🧠 为什么会有 默认系统类加载器 和 自定义类加载器？
>
>   因为有些场景，系统类加载器无法满足我们的需求。
>   也就是说，只有在特殊场景下才会使用 `xxxClassLoader` 自定义加载，而不是默认的 `SystemClassLoader`。
>
>   🔍 对比：两种加载方式的差别
>
>   | 加载方式                                         | 说明                                           | 适用场景                                 |
>   | ------------------------------------------------ | ---------------------------------------------- | ---------------------------------------- |
>   | `ClassLoader.getSystemClassLoader().loadClass()` | 使用默认的系统类加载器加载类，遵循双亲委派机制 | 加载classpath中的类，普通情况足够        |
>   | `xxxClassLoader.loadClass()`                     | 使用自定义类加载器，跳过/控制双亲委派机制      | 热部署、隔离插件、加载加密类、类版本冲突 |
>
> - 🚩 为什么不用系统类加载器？
>
>   - 场景 1️⃣：类隔离（插件系统）
>
>     假设有两个插件，它们都有一个叫 `com.example.PluginMain` 的类，但它们功能不同。
>
>     如果都用系统类加载器，只能加载一次，第二个插件会重用第一个插件的类，导致冲突。
>
>     解决办法：为每个插件用一个独立的类加载器。
>
>     ```
>     ClassLoader plugin1Loader = new MyClassLoader(plugin1Path);
>     Class<?> plugin1Class = plugin1Loader.loadClass("com.example.PluginMain");
>     
>     ClassLoader plugin2Loader = new MyClassLoader(plugin2Path);
>     Class<?> plugin2Class = plugin2Loader.loadClass("com.example.PluginMain");
>     ```
>
>   - 场景 2️⃣：类热加载 / 卸载
>
>     系统类加载器加载的类无法被卸载，因为它在 JVM 生命周期中常驻。
>
>     如果你想重新加载修改过的类（比如调试时替换了 TargetObject.class），就必须用自定义类加载器（每次都 new 一个新的）：
>
>     ```java
>     ClassLoader hotLoader = new MyHotReloadClassLoader();
>     Class<?> newClass = hotLoader.loadClass("cn.javaguide.TargetObject");
>     ```
>
>   - 场景 3️⃣：加载非 `classpath` 下的类
>
>     系统类加载器只认 `classpath` 下的类。你想从磁盘/网络/加密 jar 加载类时，系统类加载器根本找不到。
>
> - ❓ 如何自定义类加载器
>
>   - 继承 `ClassLoader`，重写 `findClass` 方法
>
>     ```java
>     public class MyClassLoader extends ClassLoader {
>         @Override
>         protected Class<?> findClass(String name) throws ClassNotFoundException {
>             // 从自定义路径中加载 class 字节码，然后 defineClass()
>             byte[] classData = new byte[0];
>             try {
>                 classData = loadClassFromDisk(name);
>             } catch (IOException e) {
>                 throw new RuntimeException(e);
>             }
>     
>             // 使用 defineClass 将 byte[] 转换为 Class 对象
>             return defineClass(name, classData, 0, classData.length);
>         }
>     
>         private byte[] loadClassFromDisk(String className) throws IOException {
>             // 获取项目编译后的 classpath 目录（即 target/classes/）
>             // 通过 URLDecoder.decode 编码 classpath 目录中可能存在的 中文 和 空格
>             String basePath = URLDecoder.decode(
>                     this.getClass().getClassLoader().getResource("").getPath(),     // 可能获取的路径中存在 中文 或 空格
>                     StandardCharsets.UTF_8.name()
>             );
>             String filePath = basePath + className.replace('.', '/') + ".class";
>     
>             FileInputStream inputStream = new FileInputStream(filePath);
>             ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
>     
>             byte[] buffer = new byte[1024];
>             int length;
>             while ((length = inputStream.read(buffer)) != -1) {
>                 outputStream.write(buffer, 0, length);
>             }
>     
>             inputStream.close();
>             return outputStream.toByteArray();
>         }
>     }
>     ```
>
>   - 使用案例
>
>     ```java
>     public class test {
>         public static void main(String[] args) throws ClassNotFoundException {
>             MyClassLoader myClassLoader = new MyClassLoader();
>     
>             // 系统默认类加载器
>             Class<?> personClazz = ClassLoader.getSystemClassLoader().loadClass("com.gc.annotation.Person");
>     
>             // 自定义类加载器
>             Class<?> personClazz1 = myClassLoader.findClass("com.gc.annotation.Person");
>     
>             System.out.println(personClazz);
>             System.out.println(personClazz1);
>         }
>     }
>     ```
>
>   - 输出结果：
>
>     ```java
>     class com.gc.annotation.Person
>     class com.gc.annotation.Person
>     ```
>
>     
>
> - 📌 补充：双亲委派模型
>
>   Java 的类加载机制是双亲委派模型：
>
>   ```
>   你调用 childLoader.loadClass("A") 
>      ↓
>   childLoader 先问 parent.loadClass("A") 
>      ↓
>   parent -> parent -> Bootstrap 尝试加载
>      ↓
>   都加载不了才由 child 自己找 A.class
>   ```
>
>   这保证了核心类不会被重复定义或“污染”。
>
>   但有时候你不想遵循双亲委派，就必须自定义类加载器并重写 loadClass() 或 findClass() 逻辑。

⚠⚠ 通过类加载器获取 Class 对象**不会进行初始化**，意味着不进行包括初始化等一系列步骤，静态代码块和静态对象不会得到执行。

> - 示例
>
>   ```java
>   public class ClassLoadDemo {
>       public static void main(String[] args) throws ClassNotFoundException {
>           System.out.println("方式1：ClassName.class");
>           Class<?> cls1 = com.gc.reflection.TargetObject.class; // ❌ 不初始化
>           System.out.println("cls1 loaded\n");
>   
>           System.out.println("方式2：Class.forName");
>           Class<?> cls2 = Class.forName("com.gc.reflection.TargetObject"); // ✅ 默认初始化
>           System.out.println("cls2 loaded\n");
>   
>           System.out.println("方式3：instance.getClass()");
>           com.gc.reflection.TargetObject obj = new com.gc.reflection.TargetObject(); // ✅ 会初始化（构造时）
>           Class<?> cls3 = obj.getClass();
>           System.out.println("cls3 loaded\n");
>   
>           System.out.println("方式4：ClassLoader.loadClass()");
>           ClassLoader loader = ClassLoader.getSystemClassLoader();
>           Class<?> cls4 = loader.loadClass("com.gc.reflection.TargetObject"); // ❌ 不初始化
>           System.out.println("cls4 loaded\n");
>       }
>   }
>   
>   ```
>
> - 输出结果：
>
>   ```java
>   方式1：ClassName.class
>   cls1 loaded
>   
>   方式2：Class.forName
>   🟢 TargetObject 的静态代码块被执行！
>   cls2 loaded
>   
>   方式3：instance.getClass()
>   🟢 TargetObject 的静态代码块被执行！// ⚠注：若完整执行上述代码，此条输出不会有，因为 方式2中 已经加载过 TargetObject 类，静态代码块只执行一次
>   🔵 TargetObject 构造方法被执行！
>   cls3 loaded
>   
>   方式4：ClassLoader.loadClass()
>   cls4 loaded
>   
>   ```
>
> - 总结：
>
>   | 获取方式                           | 会初始化？ | 原因说明                                | 用途                         |
>   | ---------------------------------- | ---------- | --------------------------------------- | ---------------------------- |
>   | `TargetObject.class`               | ❌ 否       | 被动使用，获取 Class 对象不会触发初始化 | 编译期确定的类引用           |
>   | `Class.forName("com.xxx")`         | ✅ 是       | 主动使用，默认初始化（除非传 false）    | 运行时通过类名加载并初始化   |
>   | `new TargetObject()`               | ✅ 是       | 主动使用：实例化对象                    |                              |
>   | `instance.getClass()`              | ✅ 是       | 先 new 才能 getClass，自然初始化        | 动态获取某个实例的 Class     |
>   | `ClassLoader.loadClass("com.xxx")` | ❌ 否       | 被动使用，仅“加载”但不初始化            | 控制加载过程（热加载、插件） |

## 2、反射的一些基本操作

### 2.1、创建一个要使用反射操作的类 `TargetObject`。

```java
public class TargetObject {
    static {
        System.out.println("🟢 TargetObject 的静态代码块被执行！");
    }

    private String value;

    public TargetObject() {
        System.out.println("🔵 TargetObject 构造方法被执行！");
    }


    // 公有方法
    public void publicMethod(String s) {
        System.out.println("🟡 TargetObject_PublicMethod: " + s);
    }

    // 私有方法
    private void privateMethod() {
        System.out.println("🔴 TargetObject_PublicMethod value is " + value);
    }
}
```

### 2.2使用反射操作这个类的方法以及属性

#### 2.2.1

```java
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectionDemo {
    public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, NoSuchFieldException {
        /**
         * 获取 TargetObject 类的 Class 对象并且创建 TargetObject 类实例
         */
        Class<?> targetClass = Class.forName("com.gc.reflection.TargetObject");
        TargetObject targetObject = (TargetObject) targetClass.newInstance();
        /**
         * 获取 TargetObject 类中定义的所有方法
         */
        Method[] methods = targetClass.getDeclaredMethods();
        System.out.println("\n🟣main-methodNames：");
        for (Method method : methods) {
            System.out.println(method.getName());
        }
        System.out.println();

        /**
         * 获取指定方法并调用
         */
        Method publicMethod = targetClass.getDeclaredMethod("publicMethod",
                String.class);

        publicMethod.invoke(targetObject, "GC——publicMethod");

        /**
         * 获取指定参数并对参数进行修改
         */
        Field field = targetClass.getDeclaredField("value");
        //为了对类中的 private参数 进行修改，取消安全检查
        field.setAccessible(true);  // 绕过 Java 的访问控制检查（如 private / protected），允许访问或修改原本不可见的字段、方法或构造器
        field.set(targetObject, "GC——Value");

        /**
         * 调用 private 方法
         */
        Method privateMethod = targetClass.getDeclaredMethod("privateMethod");
        //为了调用 private方法，取消安全检查
        privateMethod.setAccessible(true);
        privateMethod.invoke(targetObject);
    }
}
```

### （3）输出内容

```
🟢 TargetObject 的静态代码块被执行！
🔵 TargetObject 构造方法被执行！

🟣main-methodNames：
publicMethod
privateMethod

🟡 TargetObject_PublicMethod: GC——publicMethod
🔴 TargetObject_PublicMethod value is GC——Value
```

