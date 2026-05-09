# 一、泛型

## 1、什么是泛型？有什么作用？

**Java 泛型（Generics）** 是 JDK 5 中引入的一个新特性。使用泛型参数，可以增强代码的可读性以及稳定性，主要目标是：

- 增强类型安全

- 在编译阶段就能发现类型错误

- 避免强制类型转换

编译器可以对泛型参数进行检测，并且通过泛型参数可以指定传入的对象类型。比如 `ArrayList<Person> persons = new ArrayList<Person>()` 这行代码就指明了该 `ArrayList` 对象只能传入 `Person` 对象，如果传入其他类型的对象就会报错。

```java
ArrayList<E> extends AbstractList<E>
```

并且，原生 `List` 返回类型是 `Object` ，需要手动转换类型才能使用，使用泛型后编译器自动转换。

## 2、泛型的使用方式有哪几种？

泛型一般有三种使用方式:**泛型类**、**泛型接口**、**泛型方法**。

**1. 泛型类**：

```java
//此处T可以随便写为任意标识，常见的如T、E、K、V等形式的参数常用于表示泛型
//在实例化泛型类时，必须指定T的具体类型
// 声明有一个<T>
public class Generic<T>{

    // 如果不声明<T>，此处会报错
    private T key;

    public Generic(T key) {
        this.key = key;
    }

    public T getKey(){
        return key;
    }
}
```

如何实例化泛型类：

```java
Generic<Integer> genericInteger = new Generic<Integer>(123456);
```



**2.泛型接口**：

```java
public interface Generator<T> {
    public T method();
}
```

实现泛型接口，不指定类型：

```java
class GeneratorImpl<T> implements Generator<T>{
    @Override
    public T method() {
        return null;
    }
}
```

实现泛型接口，指定类型：

```java
class GeneratorImpl implements Generator<String> {
    @Override
    public String method() {
        return "hello";
    }
}
```



**3.泛型方法**：

```java
public static < E > void printArray( E[] inputArray )
{
      for ( E element : inputArray ){
         System.out.printf( "%s ", element );
      }
      System.out.println();
 }
```

使用：

```java
// 创建不同类型数组：Integer, Double 和 Character
Integer[] intArray = { 1, 2, 3 };
String[] stringArray = { "Hello", "World" };
printArray( intArray  );
printArray( stringArray  );
```

> ⚠注意: 
>
> - `public static < E > void printArray( E[] inputArray )` 一般被称为静态泛型方法；在 java 中泛型只是一个占位符，必须在传递类型后才能使用。
> - 类在实例化时才能真正的传递类型参数，由于静态方法的加载先于类的实例化，也就是说类中的泛型还没有传递真正的类型参数，静态的方法的加载就已经完成了，所以静态泛型方法是没有办法使用类上声明的泛型的。只能使用自己声明的 `<E>`

## 3、**什么是泛型擦除机制？为什么要擦除?**

Java 的泛型是 **伪泛型**，这是因为 Java 在编译期间，**所有的** 泛型信息都会被擦掉，这也就是通常所说类型擦除 。

编译器会在编译期间会动态地将泛型 `T` 擦除为 `Object` ，或将 `T extends xxx` 擦除为其限定类型 `xxx` 。

⚠因此，泛型本质上其实还是 **编译器的行为**，为了保证引入泛型机制但不创建新的类型，减少虚拟机的运行开销，编译器通过擦除将泛型类转化为一般类。

类型擦除的具体过程（编译器行为）：

- 示例 1：无边界类型参数

  ```java
  List<String> strList = new ArrayList<>();
  ```

  类型擦除后：

  ```java
  List strList = new ArrayList();
  ```

- 示例 2：有边界的泛型参数

  ```java
  public <T extends Number> void print(T t) { }
  ```

  类型擦除后：

  ```java
  public void print(Number t) { }
  ```

  也就是说，`T` 被擦除成它的上界 `Number`。

- 示例3：方法中使用了泛型

  ```java
  public class Box<T> {
      private T value;
  
      public void set(T value) {
          this.value = value;
      }
  
      public T get() {
          return value;
      }
  }
  ```

  类型擦除后：

  ```java
  public class Box {
      private Object value;
  
      public void set(Object value) {
          this.value = value;
      }
  
      public Object get() {
          return value;
      }
  }
  ```

  ⚠⚠也就是说，泛型类型参数`<T>`  被直接擦除，泛型变量 `T` 则被改为 `Object`

  

⚠ 因此，在编译期间，通过泛型限制类型，达到类型安全的目的。

✅这里说的可能有点抽象，举个例子：

- 编译期间，添加错误类型，直接报错

  ```java
  List<Integer> list = new ArrayList<>();
  
  list.add(12);
  //1.编译期间直接添加会报错
  list.add("a");
  ```

- 运行期间，通过反射，可以添加任意类型的数据到List中

  ```java
  Class<? extends List> clazz = list.getClass();
  Method add = clazz.getDeclaredMethod("add", Object.class);
  //2.运行期间通过反射添加，是可以的
  add.invoke(list, "kl");
  
  System.out.println(list)
  ```

  但是后续遍历的时候，如果类型错误，会导致 `ClassCastException`。

✅再来举一个例子 : 由于泛型擦除的问题，下面的方法重载会报错。

```java
public void print(List<String> list)  { }
public void print(List<Integer> list) { }
```

原因也很简单，泛型擦除之后，`List<String>` 与 `List<Integer>` 在编译以后都变成了 `List` ，不构成方法的重载。

所以这两个方法在字节码层看起来完全一样：

```java
public void print(List list) { } // 被擦除后的样子
public void print(List list) { } // 冲突
```

✅ 正确的重载方式
如果你希望根据元素类型区分处理，可以考虑以下替代方案：

✅ 方法名不同（推荐方式）：

```java
public void printStringList(List<String> list) { }
public void printIntegerList(List<Integer> list) { }
```

✅ 运行时判断类型（不推荐，但可行）：

```java
public void print(List<?> list) {
    if (!list.isEmpty()) {
        Object first = list.get(0);
        if (first instanceof String) {
            System.out.println("String list");
        } else if (first instanceof Integer) {
            System.out.println("Integer list");
        }
    }
}

```



❓**既然编译器要把泛型擦除，那为什么还要用泛型呢？用 Object 代替不行吗？**

这个问题其实在变相考察泛型的作用：

- 使用泛型可在编译期间进行类型检测。 
- 使用 `Object` 类型需要手动添加强制类型转换，降低代码可读性，提高出错概率。 
- 泛型可以使用自限定类型如 `T extends Comparable` 。 



## 4、**什么是桥方法？**

桥方法(Bridge Method) 用于继承泛型类时保证多态，目的是为了在类型擦除后，保持子类与父类方法的多态性一致。

### （1）🔍 为什么需要桥方法？

**泛型 + 类型擦除 + 方法重写** 可能会导致“重写失效”。

看个例子：

```java
class Parent<T> {
    public T getValue() {
        return null;
    }
}

class Child extends Parent<String> {
    @Override
    public String getValue() {
        return "hello";
    }
}

```

**问题来了！**
泛型擦除后，这两个方法会变成：

```java
// Parent 类
public Object getValue() { ... }

// Child 类
public String getValue() { ... } // 注意签名变了！

```

❗这就不是重写了，而是“重载”！

> “重载” 是因为这个案例中，参数列表为空，不符合重载的定义，但是签名变了。

所以：

- 编译器会提示“子类没有重写父类方法”

- 或者运行时多态调用失败

### （2）✅ 编译器如何解决？生成桥方法！

```java
// 桥方法，由编译器生成
public Object getValue() {
    return getValue(); // 调用的是下面子类的 String 版本
}

// 自己写的重写方法
public String getValue() {
    return "hello";
}

```

这样：

- 子类同时拥有 `Object getValue()` 和 `String getValue()` 两个方法

- 当父类引用调用 getValue()，仍然会正确走到子类的逻辑

### （3）如何看到桥方法？

可以通过反射看到桥方法：

```java
for (Method method : Child.class.getMethods()) {
    if (method.isBridge()) {
        System.out.println("桥方法: " + method);
    }
}
```

### （4）桥方法总结

| 问题                     | 桥方法解决了什么                                            |
| ------------------------ | ----------------------------------------------------------- |
| 泛型擦除导致方法签名改变 | 桥方法补上擦除后的方法，使得重写继续成立                    |
| 保证多态机制在泛型下生效 | 子类即使方法签名不同，也能通过桥方法被父类引用调用          |
| Java 编译器自动生成      | 是合成方法（synthetic + bridge），你写不出来，但 JVM 执行它 |

完整示例代码：

```java
class Node<T> {
    public T data;
    public Node(T data) { this.data = data; }
    public void setData(T data) {
        System.out.println("Node.setData");
        this.data = data;
    }
}

class MyNode extends Node<Integer> {
    public MyNode(Integer data) { super(data); }

  	// Node<T> 泛型擦除后为 setData(Object data)，而子类 MyNode 中并没有重写该方法，所以编译器会加入该桥方法保证多态
   	public void setData(Object data) {
        // 调用子类的方法，保证在父类引用调用该方法时，仍能调用子类的方法
        setData((Integer) data);
    }

    public void setData(Integer data) {
        System.out.println("MyNode.setData");
        super.setData(data);
    }
}
```

⚠️注意 ：桥方法为编译器自动生成，非手写。



## 5、泛型有哪些限制？为什么？

泛型的限制一般是由泛型擦除机制导致的。擦除为 `Object` 后无法进行类型判断

- 只能声明不能实例化 `T` 类型变量。
- 泛型参数不能是基本类型。因为基本类型不是 `Object` 子类，应该用基本类型对应的引用类型代替。
- 不能实例化泛型参数的数组。擦除后为 `Object` 后无法进行类型判断。
- 不能实例化泛型数组。
- 泛型无法使用 `Instance of` 和 `getClass()` 进行类型判断。
- 不能实现两个不同泛型参数的同一接口，擦除后多个父类的桥方法将冲突
- 不能使用 `static` 修饰泛型变量
  - 因为 `T` 是类的泛型参数，它是在实例化类的时候才确定的。
  - 而 `static` 成员是属于类本身的，在类加载时就存在。
  - 所以 `static` 和泛型参数的生命周期不一致，编译器无法知道 `T` 是什么类型，因此不允许这么写。
- ......



## 6、**以下代码是否能编译，为什么？**

```java
public final class Algorithm {
    public static <T> T max(T x, T y) {
        return x > y ? x : y;
    }
}
```

无法编译，因为 x 和 y 都会被擦除为 `Object` 类型， `Objec`t 无法使用 `>` 进行比较

```java
public class Singleton<T> {

    public static T getInstance() {
        if (instance == null)
            instance = new Singleton<T>();

        return instance;
    }

    private static T instance = null;
}
```

无法编译，因为不能使用 `static` 修饰泛型 `T`。

```java
public void test(List<T> list) {}          // ❌ 错误：T 未声明
public <T> void test(List<T> list) {}      // ✅ 正确：声明了 T 是泛型参数
public <T extends Number> void test(List<T> list) {} // ✅ 加上了类型上界限制

```



## 7、项目中哪里用到了泛型？

- 自定义接口通用返回结果 `CommonResult<T>` 通过参数 `T` 可根据具体的返回类型动态指定结果的数据类型

  ```java
  public class CommonResult<T> {
      private int code;
      private String message;
      private T data;
  
      public CommonResult(int code, String message, T data) {
          this.code = code;
          this.message = message;
          this.data = data;
      }
  
      public static <T> CommonResult<T> success(T data) {
          return new CommonResult<>(200, "成功", data);
      }
  
      public static <T> CommonResult<T> error(String message) {
          return new CommonResult<>(500, message, null);
      }
  
      // Getter & Setter
      public int getCode() { return code; }
      public String getMessage() { return message; }
      public T getData() { return data; }
  }
  
  ```

  

- 定义 `Excel` 处理类 `ExcelUtil<T>` 用于动态指定 `Excel` 导出的数据类型

  - 此类假设使用第三方库（如 Apache POI 或 EasyExcel），这里只写结构和伪实现：

    ```java
    import java.util.List;
    
    public class ExcelUtil<T> {
    
        private Class<T> clazz;
    
        public ExcelUtil(Class<T> clazz) {
            this.clazz = clazz;
        }
    
        public void export(List<T> dataList, String filePath) {
            System.out.println("导出 Excel：" + filePath);
            System.out.println("数据类型：" + clazz.getSimpleName());
            for (T data : dataList) {
                System.out.println("行数据：" + data);
            }
        }
    }
    
    ```

  - 示例实体类：

    ```java
    public class User {
        private String name;
        private int age;
    
        public User(String name, int age) { this.name = name; this.age = age; }
        public String toString() { return name + " - " + age; }
    }
    
    ```

  - 使用示例：

    ```java
    List<User> userList = Arrays.asList(new User("张三", 25), new User("李四", 30));
    ExcelUtil<User> util = new ExcelUtil<>(User.class);
    util.export(userList, "users.xlsx");
    
    ```

  

- 构建集合工具类（参考 `Collections` 中的 `sort`, `binarySearch` 方法）。

  - 示例：

    ```java
    import java.util.Collections;
    import java.util.Comparator;
    import java.util.List;
    
    public class MyCollections {
    
        public static <T extends Comparable<? super T>> void sort(List<T> list) {
            Collections.sort(list);
        }
    
        public static <T> int binarySearch(List<? extends Comparable<? super T>> list, T key) {
            return Collections.binarySearch((List) list, key);
        }
    }
    
    ```

  - 使用示例：

    ```java
    List<Integer> nums = Arrays.asList(9, 5, 2, 8);
    MyCollections.sort(nums);
    System.out.println("排序后：" + nums);  // [2, 5, 8, 9]
    
    int index = MyCollections.binarySearch(nums, 5);
    System.out.println("查找 5 的索引：" + index);  // 1
    
    ```

- ……

# 二、泛型变量 和 通配符 `?`

## 1、命名约定（推荐使用的泛型变量命名）

虽然可以随便命名，只要求必须是 **合法的 Java 标识符** 即可，但通常使用单个大写字母代表特定含义：

| 占位符 | 含义                |
| ------ | ------------------- |
| `T`    | Type，类型          |
| `E`    | Element，集合的元素 |
| `K`    | Key，键             |
| `V`    | Value，值           |
| `N`    | Number，数字        |
| `R`    | Result，返回值      |

## 2、通配符 `?`（和泛型变量不同）

**什么是通配符？有什么作用？**

泛型类型是固定的，某些场景下使用起来不太灵活，于是，通配符就来了！通配符可以允许类型参数变化，用来解决泛型无法协变的问题。

举个例子：

```java
// 限制类型为 Person 的子类
<? extends Person>
// 限制类型为 Manager 的父类
<? super Manager>
```

## 3、**通配符 ？和常用的泛型 T 之间有什么区别？**

- `T` 可以用于声明变量或常量，而 `?` 不行。
- `T` 一般用于声明泛型类或方法，通配符 `?` 一般用于泛型方法的调用代码和形参。
- `T` 在编译期会被擦除为限定类型或 `Object`，通配符用于捕获具体类型。

示例：

```java
public void printList(List<?> list) {
    for (Object obj : list) {
        System.out.println(obj);
    }
}
```

## 4、**什么是无界通配符？**

无界通配符可以接收任何泛型类型数据，用于实现不依赖于具体类型参数的简单方法，可以捕获参数类型并交由泛型方法进行处理。

```java
void testMethod(Person<?> p) {
  // 泛型方法自行处理
}
```

**`List<?>` 和 `List` 有区别吗？** 

**当然有！**

- `List<?> list` 表示 `list` 是持有某种特定类型的 `List`，但是不知道具体是哪种类型。因此，我们添加元素进去的时候会 **报错**。
- `List list` 表示 `list` 是持有的元素的类型是 `Object`，因此可以添加任何类型的对象，只不过编译器会有 **警告信息**。

## 5、使用约束（上下限）

在使用泛型的时候，我们还可以为传入的泛型类型实参进行上下边界的限制，如：类型实参只准传入某种类型的父类或某种类型的子类。

- **上边界** 通配符 `extends` 可以实现泛型的向上转型，即传入的类型实参必须是指定类型的子类型。

  通配符边界：

  ```java
  // 限制必须是 Person 类的子类
  <? extends Person>
  ```

  类型边界可以设置多个，还可以对 `T` 类型进行限制。

  ```java
  <T extends T1 & T2>
  <T extends XXX>
  ```

  表示：类型参数 T 必须是 T1 的子类，同时实现接口 T2。

  > 注意：
  > Java 要求：
  >
  > - 第一个类型（T1）必须是类（可以是 Object），即 Java 规定多重上界中最多只能有一个类【单继承多实现】
  >
  > - 后面的必须是接口

  示例：

  ```java
  interface A {
      void a();
  }
  
  interface B {
      void b();
  }
  
  class MyClass implements A, B {
      public void a() {
          System.out.println("a()");
      }
      public void b() {
          System.out.println("b()");
      }
  }
  
  public class Demo<T extends A & B> {
      private T obj;
  
      public Demo(T obj) {
          this.obj = obj;
      }
  
      public void use() {
          obj.a();
          obj.b();
      }
  
      public static void main(String[] args) {
          MyClass my = new MyClass();
          Demo<MyClass> demo = new Demo<>(my);
          demo.use();
      }
  }
  
  ```

  输出结果：

  ```java
  a()
  b()
  ```

  ❌ 常见错误示例：

  ```java
  <T extends A & B & Base> // 错误！
  ```

  > ❗编译错误！Java 规定多重上界中最多只能有一个类，并且类必须在最前面。

- **下边界** 通配符 `super` 与上边界通配符 extends刚好相反，它可以实现泛型的向下转型即传入的类型实参必须是指定类型的父类型。

  举个例子：

  ```java
  //  限制必须是 Employee 类的父类
  List<? super Employee>
  ```

### （1）`? extends xxx`和 `? super xxx` 有什么区别?

- 两者接收参数的范围不同

- 使用 `? extends xxx` 声明的泛型参数，只能调用 `get()` 方法返回 xxx 类型，调用 set() 报错。

  `? extends T` — **只读**容器（只能 get）

  示例：

  ```java
  List<? extends Number> list;
  ```

  > 表示：list 是某种 `Number` 的子类型的列表，但我们不知道是具体哪个子类（可能是`Integer`、`Double`…）。

  - ✅ 可以做什么？

    ```java
    Number n = list.get(0); // ✅ 返回值至少是 Number 类型
    ```

  - ❌ 不能做什么？

    ```java
    list.add(123);     // ❌ 编译错误！
    list.add(new Object()); // ❌ 编译错误！
    ```

  - ❓ 为什么不能 add()？

    因为 `list` 可能是 `List<Double>`，你往里面塞 `Integer` 就错了。为了保证类型安全，Java 编译器禁止你 add 任何非 `null` 的对象进去。

- 使用 `? super xxx` 声明的泛型参数，只能调用 `set()` 方法接收 xxx 类型，调用 get() 报错。

  `? super T` — **只写**容器（只能 set）

  示例：

  ```java
  List<? super Integer> list;
  ```

  > 表示：list 是 `Integer` 的某个超类型的列表，比如：`List<Integer>`、`List<Number>`、`List<Object>`。

  - ✅ 可以做什么？

    ```java
    list.add(123); // ✅ 安全！Integer 及其子类都能加
    ```

  - ❌ 不能做什么？

    ```java
    Integer x = list.get(0); // ❌ 编译错误！
    ```

    > 因为你只知道它是某个超类的容器，get 出来的可能是 Object，也可能是 Number，Java 编译器不敢断言一定是 Integer。

⚠️总结：

| 声明方式            | 可以添加什么？      | 返回时 get() 是什么类型？ | 使用场景         |
| ------------------- | ------------------- | ------------------------- | ---------------- |
| `List<? extends T>` | ❌（不能安全添加）   | ✅ 至少是 `T` 类型         | 只读场景（遍历） |
| `List<? super T>`   | ✅ 可添加 `T` 及子类 | ❌ get() 只能是 `Object`   | 只写场景（填充） |

### （2）`T extends xxx` 和 `? extends xxx` 又有什么区别？

- `T extends Xxx` 是用于定义泛型类或方法的类型参数，表示 `T` 至少是 Xxx，擦除后为 Xxx。

- `? extends Xxx` 是用于泛型类型的使用位置（如方法参数、变量声明），表示接受 Xxx 或其子类，但无法安全地添加元素。

### （3）`T` 是否有读写限制

| 写法            | 属于       | 是否支持读写  | 备注                        |
| --------------- | ---------- | ------------- | --------------------------- |
| `T extends Xxx` | ✅ 泛型定义 | ✅ 可读 ✅ 可写 | 擦除后为 Xxx 类型           |
| `T super Xxx`   | ❌ 不支持   | ❌ 不允许      | Java 不支持                 |
| `? extends Xxx` | ✅ 泛型使用 | ✅ 读 ❌ 写     | 生产者，仅读取（PECS 原则） |
| `? super Xxx`   | ✅ 泛型使用 | ❌ 读 ✅ 写     | 消费者，仅写入              |

### （4）`Class<?>` 和 `Class` 的区别？

直接使用 `Class` 的话会有一个类型警告，使用 `Class<?>` 则没有，因为 Class 是一个泛型类，接收原生类型会产生警告。

## 6、**以下代码是否能编译，为什么？**

```java
class Shape { /* ... */ }
class Circle extends Shape { /* ... */ }
class Rectangle extends Shape { /* ... */ }

class Node<T> { /* ... */ }

Node<Circle> nc = new Node<>();
Node<Shape>  ns = nc;
```

❌不能，因为`Node<Circle>` 不是 `Node<Shape>` 的子类

```java
class Shape { /* ... */ }
class Circle extends Shape { /* ... */ }
class Rectangle extends Shape { /* ... */ }

class Node<T> { /* ... */ }
class ChildNode<T> extends Node<T>{

}
ChildNode<Circle> nc = new ChildNode<>();
Node<Circle>  ns = nc;
```

✅ 可以编译，ChildNode`<Circle>` 是 Node`<Circle>` 的子类

```java
public static void print(List<? extends Number> list) {
    for (Number n : list)
        System.out.print(n + " ");
    System.out.println();
}
```

✅可以编译，`List<? extends Number>` 可以往外取元素，但是无法调用 add() 添加元素。
