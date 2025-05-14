# 一、面向对象基础

## 1、面向对象和面向过程的区别

面向过程编程（Procedural-Oriented Programming，POP）和面向对象编程（Object-Oriented Programming，OOP）是两种常见的编程范式，两者的主要区别在于解决问题的方式不同：

- **面向过程编程（POP）**：面向过程把解决问题的过程拆成一个个方法，通过一个个方法的执行解决问题。
- **面向对象编程（OOP）**：面向对象会先抽象出对象，然后用对象执行方法的方式解决问题。

相比较于 POP，OOP 开发的程序一般具有下面这些优点：

- **易维护**：由于良好的结构和封装性，OOP 程序通常更容易维护。
- **易复用**：通过继承和多态，OOP 设计使得代码更具复用性，方便扩展功能。
- **易扩展**：模块化设计使得系统扩展变得更加容易和灵活。

POP 的编程方式通常更为简单和直接，适合处理一些较简单的任务。

POP 和 OOP 的性能差异主要取决于它们的运行机制，而不仅仅是编程范式本身。因此，简单地比较两者的性能是一个常见的误区。

在选择编程范式时，性能并不是唯一的考虑因素。代码的可维护性、可扩展性和开发效率同样重要。

现代编程语言基本都支持多种编程范式，既可以用来进行面向过程编程，也可以进行面向对象编程。

下面是一个求圆的面积和周长的示例，简单分别展示了面向对象和面向过程两种不同的解决方案。

**面向对象**：

```java
public class Circle {
    // 定义圆的半径
    private double radius;

    // 构造函数
    public Circle(double radius) {
        this.radius = radius;
    }

    // 计算圆的面积
    public double getArea() {
        return Math.PI * radius * radius;
    }

    // 计算圆的周长
    public double getPerimeter() {
        return 2 * Math.PI * radius;
    }

    public static void main(String[] args) {
        // 创建一个半径为3的圆
        Circle circle = new Circle(3.0);

        // 输出圆的面积和周长
        System.out.println("圆的面积为：" + circle.getArea());
        System.out.println("圆的周长为：" + circle.getPerimeter());
    }
}
```

我们定义了一个 `Circle` 类来表示圆，该类包含了圆的半径属性和计算面积、周长的方法。

**面向过程**：

```java
public class Main {
    public static void main(String[] args) {
        // 定义圆的半径
        double radius = 3.0;

        // 计算圆的面积和周长
        double area = Math.PI * radius * radius;
        double perimeter = 2 * Math.PI * radius;

        // 输出圆的面积和周长
        System.out.println("圆的面积为：" + area);
        System.out.println("圆的周长为：" + perimeter);
    }
}
```

我们直接定义了圆的半径，并使用该半径直接计算出圆的面积和周长。

## 2、创建一个对象用什么运算符?对象实体与对象引用有何不同?

`new` 运算符，`new` 创建对象实例（对象实例在 堆内存 中），对象引用指向对象实例（对象引用存放在 栈内存 中）。

- 一个对象引用可以指向 0 个或 1 个对象（一根绳子可以不系气球，也可以系一个气球）；
- 一个对象可以有 n 个引用指向它（可以用 n 条绳子系住一个气球）。

## 3、对象的相等和引用相等的区别

- 对象的相等一般比较的是内存中存放的内容是否相等。
- 引用相等一般比较的是他们指向的内存地址是否相等。

```java
String str1 = "hello";
String str2 = new String("hello");
String str3 = "hello";
// 使用 == 比较字符串的引用相等
System.out.println(str1 == str2);
System.out.println(str1 == str3);	// str1和str3都执行了字符串常量池中的 "hello"
// 使用 equals 方法比较字符串的相等
System.out.println(str1.equals(str2));
System.out.println(str1.equals(str3));
```

输出结果：

```
false
true
true
true
```

## 4、如果一个类没有声明构造方法，该程序能正确执行吗?

构造方法是一种特殊的方法，主要作用是完成对象的初始化工作。

如果一个类没有声明构造方法，也可以执行！因为一个类即使没有声明构造方法，也会有默认的不带参数的构造方法。如果我们自己添加了类的构造方法（无论是否有参），Java 就不会添加默认的无参数的构造方法了。

我们一直在不知不觉地使用构造方法，这也是为什么我们在创建对象的时候后面要加一个括号（因为要调用无参的构造方法）。如果我们重载了有参的构造方法，记得都要把无参的构造方法也写出来（无论是否用到），因为这可以帮助我们在创建对象的时候少踩坑

## 5、构造方法有哪些特点？是否可被 override?

构造方法具有以下特点：

- **名称与类名相同**：构造方法的名称必须与类名完全一致。
- **没有返回值**：构造方法没有返回类型，且不能使用 `void` 声明。
- **自动执行**：在生成类的对象时，构造方法会自动执行，无需显式调用。

构造方法**不能被重写（override）**，但**可以被重载（overload）**。因此，一个类中可以有多个构造方法，这些构造方法可以具有不同的参数列表，以提供不同的对象初始化方式。

```java
public class People {
    private String name;
    private int age;

    //无参构造
    public People() {
        System.out.println("一个人的诞生");
    }
    //有参构造方法
    public People(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public static void main(String[] args) {
        People people = new People();
    }
}
```

## 6、面向对象三大特征
- **封装**

  封装是指把一个对象的状态信息（也就是属性）隐藏在对象内部，不允许外部对象直接访问对象的内部信息。但是可以提供一些可以被外界访问的方法来操作属性。就好像我们看不到挂在墙上的空调的内部的零件信息（也就是属性），但是可以通过遥控器（方法）来控制空调。如果属性不想被外界访问，我们大可不必提供方法给外界访问。但是如果一个类没有提供给外界访问的方法，那么这个类也没有什么意义了。就好像如果没有空调遥控器，那么我们就无法操控空凋制冷，空调本身就没有意义了（当然现在还有很多其他方法 ，这里只是为了举例子）。

  ```java
  public class Student {
      private int id;//id属性私有化
      private String name;//name属性私有化
  
      //获取id的方法
      public int getId() {
          return id;
      }
  
      //设置id的方法
      public void setId(int id) {
          this.id = id;
      }
  
      //获取name的方法
      public String getName() {
          return name;
      }
  
      //设置name的方法
      public void setName(String name) {
          this.name = name;
      }
  }
  ```

- **继承**

  不同类型的对象，相互之间经常有一定数量的共同点。例如，小明同学、小红同学、小李同学，都共享学生的特性（班级、学号等）。同时，每一个对象还定义了额外的特性使得他们与众不同。例如小明的数学比较好，小红的性格惹人喜爱；小李的力气比较大。继承是使用已存在的类的定义作为基础建立新类的技术，新类的定义可以增加新的数据或新的功能，也可以用父类的功能，但不能选择性地继承父类。通过使用继承，可以快速地创建新的类，可以提高代码的重用，程序的可维护性，节省大量创建新类的时间 ，提高我们的开发效率。

  **关于继承如下 3 点请记住：**

  - 子类拥有父类对象所有的属性和方法（包括私有属性和私有方法），但是父类中的 私有属性和方法子类 是无法访问，**只是拥有**。
  - 子类可以拥有自己属性和方法，即子类可以对父类进行扩展。
  - 子类可以用自己的方式实现父类的方法。（抽象类的子实现类）。

- **多态**

  顾名思义，表示一个对象具有多种的状态，具体表现为父类的引用指向子类的实例。

  **多态的特点:**

  - 对象类型和引用类型之间具有继承（类）/实现（接口）的关系；
  - 引用类型变量发出的方法调用的到底是哪个类中的方法，必须在程序运行期间才能确定；

  - 多态不能调用“只在子类存在但在父类不存在”的方法；

  - 如果子类重写了父类的方法，真正执行的是子类重写的方法，如果子类没有重写父类的方法，执行的是父类的方法。

## 7、接口和抽象类有什么共同点和区别

### （1）共同点

- **实例化**：接口和抽象类都不能直接实例化，只能被实现（接口）或继承（抽象类）后才能创建具体的对象。
- **抽象方法**：接口和抽象类都可以包含抽象方法。抽象方法没有方法体，必须在子类或实现类中实现。

### （2）区别

- **设计目的**：接口主要用于对类的行为进行约束，你实现了某个接口就具有了对应的行为。抽象类主要用于代码复用，强调的是所属关系。

- **继承和实现**：一个类只能继承一个类（包括抽象类），因为 Java 不支持多继承。但一个类可以实现多个接口，一个接口也可以继承多个其他接口。

- **成员变量**：接口中的成员变量只能是 `public static final` 类型的，不能被修改且必须有初始值。抽象类的成员变量可以有任何修饰符（`private`, `protected`, `public`），可以在子类中被重新定义或赋值。

  > 案例如下：
  >
  > ```java
  > // 抽象父类
  > abstract class Animal {
  >     private String privateName = "private-name";
  >     protected String protectedName = "protected-name";
  >     public String publicName = "public-name";
  > 
  >     public void printNames() {
  >         System.out.println("Animal:");
  >         System.out.println("  privateName = " + privateName);
  >         System.out.println("  protectedName = " + protectedName);
  >         System.out.println("  publicName = " + publicName);
  >     }
  > }
  > 
  > // 子实现类
  > class Dog extends Animal {
  >     // 重新定义一个同名的成员变量（不会覆盖父类的 private 字段，只是隐藏）
  >     private String privateName = "dog's own private name";
  > 
  >     public Dog() {
  >         // 修改 protected 成员变量（可直接访问）
  >         this.protectedName = "dog's protected name";
  >         // 修改 public 成员变量（可直接访问）
  >         this.publicName = "dog's public name";
  >     }
  > 
  >     public void printDogNames() {
  >         System.out.println("Dog:");
  >         System.out.println("  privateName = " + privateName); // 访问自己的变量
  >         System.out.println("  protectedName = " + protectedName); // 访问继承的
  >         System.out.println("  publicName = " + publicName); // 访问继承的
  >     }
  > }
  > 
  > 
  > 
  > // 测试类
  > public class Main {
  >     public static void main(String[] args) {
  >         Dog d = new Dog();
  >         d.printNames();     // 调用父类方法
  >         d.printDogNames();  // 调用子类方法
  >     }
  > }
  > ```
  >
  > 输出结果：
  >
  > ```java
  > Animal:
  >   privateName = private-name         // 父类的私有变量，只能父类方法内部访问
  >   protectedName = dog's protected name
  >   publicName = dog's public name
  > 
  > Dog:
  >   privateName = dog's own private name   // 子类自己的变量，隐藏了父类的 privateName
  >   protectedName = dog's protected name   // 子类直接访问并修改
  >   publicName = dog's public name         // 子类直接访问并修改
  > 
  > ```
  >
  > 

- **方法**： 

  - Java 8 之前，接口中的方法默认是 `public abstract` ，也就是只能有方法声明，且实现类中**必须重写**。自 Java 8 起，可以在接口中定义 `default`（默认） 方法和 `static` （静态）方法。 自 Java 9 起，接口可以包含 `private` 方法。

    > 案例如下：
    >
    > ```java
    > // 接口
    > interface MyInterface {
    >     // 抽象方法（默认 public abstract）
    >     void abstractMethod();
    > 
    >     // default 方法（实例可继承）
    >     default void defaultMethod() {
    >         System.out.println("Default method in interface");
    >         helperMethod(); // 可以调用私有方法（Java 9 特性）
    >     }
    > 
    >     // static 方法（只能通过接口名调用）
    >     static void staticMethod() {
    >         System.out.println("Static method in interface");
    >     }
    > 
    >     // Java 9+ 中支持的私有方法
    >     // private void helperMethod() {
    >     //     System.out.println("Private method in interface");
    >     // }
    > }
    > 
    > 
    > 
    > 
    > 
    > // 实现类
    > class MyClass implements MyInterface {
    >     @Override
    >     public void abstractMethod() {
    >         System.out.println("Implemented abstract method");
    >     }
    > }
    > 
    > 
    > 
    > 
    > 
    > // 测试类
    > public class Main {
    >     public static void main(String[] args) {
    >         MyClass obj = new MyClass();
    >         obj.abstractMethod();   // 调用实现的抽象方法
    >         obj.defaultMethod();    // 调用接口中的默认方法
    > 
    >         MyInterface.staticMethod(); // 只能通过接口名调用静态方法
    >     }
    > }
    > ```
    >
    > 

  - 抽象类可以包含抽象方法和非抽象方法。抽象方法没有方法体，必须在子类中实现。非抽象方法有具体实现，可以直接在抽象类中使用或在子类中重写。

    > 案例如下：
    >
    > ```java
    > // 抽象类
    > abstract class Animal {
    >     // 抽象方法（必须由子类实现）
    >     public abstract void makeSound();
    > 
    >     // 非抽象方法（有默认实现，子类可重写也可直接使用）
    >     public void breathe() {
    >         System.out.println("Animal breathes with lungs");
    >     }
    > }
    > 
    > 
    > 
    > // 不重写非抽象方法的子类
    > class Dog extends Animal {
    >     @Override
    >     public void makeSound() {
    >         System.out.println("Dog barks");
    >     }
    > 
    >     // 不重写 breathe()，使用父类的实现
    > }
    > 
    > // 重写非抽象方法的子类
    > class Bird extends Animal {
    >     @Override
    >     public void makeSound() {
    >         System.out.println("Bird chirps");
    >     }
    > 
    >     // 选择重写非抽象方法
    >     @Override
    >     public void breathe() {
    >         System.out.println("Bird breathes through lungs and air sacs");
    >     }
    > }
    > ```

  在 Java 8 及以上版本中，接口引入了新的方法类型：`default` 方法、`static` 方法和 `private` 方法( Java 9 引入`private`)。这些方法让接口的使用更加灵活。

  Java 8 引入的`default` 方法用于提供接口方法的默认实现，可以在实现类中被覆盖。这样就可以在不修改实现类的情况下向现有接口添加新功能，从而增强接口的扩展性和向后兼容性。

  ```java
  public interface MyInterface {
      default void defaultMethod() {
          System.out.println("This is a default method.");
      }
  }
  ```

  Java 8 引入的`static` 方法无法在实现类中被覆盖，只能通过接口名直接调用（ `MyInterface.staticMethod()`），类似于类中的静态方法。`static` 方法通常用于定义一些通用的、与接口相关的工具方法，一般很少用。

  ```java
  public interface MyInterface {
      static void staticMethod() {
          System.out.println("This is a static method in the interface.");
      }
  }
  ```

  Java 9 允许在接口中使用 `private` 方法。`private`方法可以用于在接口内部共享代码，不对外暴露。

  ```java
  public interface MyInterface {
      // default 方法
      default void defaultMethod() {
          commonMethod();	// 静态私有方法
          instanceCommonMethod();	// 实例私有方法
      }
  
      // static 方法
      static void staticMethod() {
          commonMethod();
      }
  
      // 私有静态方法，可以被 static 和 default 方法调用
      private static void commonMethod() {
          System.out.println("This is a private method used internally.");
      }
  
        // 实例私有方法，只能被 default 方法调用。
      private void instanceCommonMethod() {
          System.out.println("This is a private instance method used internally.");
      }
  }
  ```

  

 ## 8、深拷贝和浅拷贝区别了解吗？什么是引用拷贝？

  关于深拷贝和浅拷贝区别，我这里先给结论：

  - **浅拷贝**：浅拷贝会在堆上创建一个新的对象（区别于引用拷贝的一点），不过，如果原对象内部的 **属性是引用类型** 的话，浅拷贝会直接复制内部对象的引用地址，也就是说拷贝对象和原对象共用同一个内部对象。
  - **深拷贝**：深拷贝会完全复制整个对象，包括这个对象所包含的内部对象。

  上面的结论没有完全理解的话也没关系，我们来看一个具体的案例！

### （1） 浅拷贝

  浅拷贝的示例代码如下，我们这里实现了 `Cloneable` 接口，并重写了 `clone()` 方法。

  `clone()` 方法的实现很简单，直接调用的是父类 `Object` 的 `clone()` 方法。

  ```java
  public class Address implements Cloneable{
      private String name;
      // 省略构造函数、Getter&Setter方法
      @Override
      public Address clone() {
          try {
              return (Address) super.clone();
          } catch (CloneNotSupportedException e) {
              throw new AssertionError();
          }
      }
  }
  
  public class Person implements Cloneable {
      private Address address;
      // 省略构造函数、Getter&Setter方法
      @Override
      public Person clone() {
          try {
              Person person = (Person) super.clone();
              return person;
          } catch (CloneNotSupportedException e) {
              throw new AssertionError();
          }
      }
  }
  ```

  测试：

  ```java
  Person person1 = new Person(new Address("武汉"));
  Person person1Copy = person1.clone();
  
  System.out.println(person1.getAddress() == person1Copy.getAddress());// true
  System.out.println(person1 == person1Copy);     // false
  ```

  从输出结构就可以看出， `person1` 的克隆对象和 `person1` 使用的仍然是同一个 `Address` 对象

### （2） 深拷贝

这里我们简单对 `Person` 类的 `clone()` 方法进行修改，连带着要把 `Person` 对象内部的 `Address` 对象一起复制。

```java
@Override
public Person clone() {
    try {
        Person person = (Person) super.clone();
        person.setAddress(person.getAddress().clone());
        return person;
    } catch (CloneNotSupportedException e) {
        throw new AssertionError();
    }
}
```

测试：

```java
Person person1 = new Person(new Address("武汉"));
Person person1Copy = person1.clone();
// false
System.out.println(person1.getAddress() == person1Copy.getAddress());
```

从输出结构就可以看出，显然 `person1` 的克隆对象和 `person1` 包含的 `Address` 对象已经是不同的了。

### （3） 引用拷贝

简单来说，引用拷贝就是两个不同的引用指向同一个对象。

### 【总结】

![](./assets/shallow&deep-copy.png)

