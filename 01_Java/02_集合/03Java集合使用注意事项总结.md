# Java结合使用注意事项总结

这篇文章我根据《阿里巴巴 Java 开发手册》总结了关于集合使用常见的注意事项以及其具体原理。

强烈建议小伙伴们多多阅读几遍，避免自己写代码的时候出现这些低级的问题。

## 1、集合判空

《阿里巴巴 Java 开发手册》的描述如下：

> **判断所有集合内部的元素是否为空，使用 `isEmpty()` 方法，而不是 `size()==0` 的方式。**

这是因为 `isEmpty()` 方法的可读性更好，并且时间复杂度为 `O(1)`。

绝大部分我们使用的集合的 `size()` 方法的时间复杂度也是 `O(1)`，不过，也有很多复杂度不是 `O(1)` 的，比如 `java.util.concurrent` 包下的 `ConcurrentLinkedQueue`。`ConcurrentLinkedQueue` 的 `isEmpty()` 方法通过 `first()` 方法进行判断，其中 `first()` 方法返回的是队列中第一个值不为 `null` 的节点（节点值为`null`的原因是在迭代器中使用的逻辑删除）

```java
public boolean isEmpty() { return first() == null; }

Node<E> first() {
    restartFromHead:
    for (;;) {
        for (Node<E> h = head, p = h, q;;) {
            boolean hasItem = (p.item != null);
            if (hasItem || (q = p.next) == null) {  // 当前节点值不为空 或 到达队尾
                updateHead(h, p);  // 将head设置为p
                return hasItem ? p : null;
            }
            else if (p == q) continue restartFromHead;
            else p = q;  // p = p.next
        }
    }
}
```

由于在插入与删除元素时，都会执行`updateHead(h, p)`方法，所以该方法的执行的时间复杂度可以近似为`O(1)`。而 `size()` 方法需要遍历整个链表，时间复杂度为`O(n)`

```java
public int size() {
    int count = 0;
    for (Node<E> p = first(); p != null; p = succ(p))
        if (p.item != null)
            if (++count == Integer.MAX_VALUE)
                break;
    return count;
}
```

此外，在`ConcurrentHashMap` 1.7 中 `size()` 方法和 `isEmpty()` 方法的时间复杂度也不太一样。`ConcurrentHashMap` 1.7 将元素数量存储在每个`Segment` 中，`size()` 方法需要统计每个 `Segment` 的数量，而 `isEmpty()` 只需要找到第一个不为空的 `Segment` 即可。但是在`ConcurrentHashMap` 1.8 中的 `size()` 方法和 `isEmpty()` 都需要调用 `sumCount()` 方法，其时间复杂度与 `Node` 数组的大小有关。下面是 `sumCount()` 方法的源码：

```java
final long sumCount() {
    CounterCell[] as = counterCells; CounterCell a;
    long sum = baseCount;
    if (as != null)
        for (int i = 0; i < as.length; ++i)
            if ((a = as[i]) != null)
                sum += a.value;
    return sum;
}
```

这是因为在并发的环境下，`ConcurrentHashMap` 将每个 `Node` 中节点的数量存储在 `CounterCell[]` 数组中。在 `ConcurrentHashMap` 1.7 中，将元素数量存储在每个`Segment` 中，`size()` 方法需要统计每个 `Segment` 的数量，而 `isEmpty()` 只需要找到第一个不为空的 `Segment` 即可。

## 2、集合转 Map

《阿里巴巴 Java 开发手册》的描述如下：

> **在使用 `java.util.stream.Collectors` 类的 `toMap()` 方法转为 `Map` 集合时，一定要注意当 value 为 null 时会抛 NPE 异常。**

```java
class Person {
    private String name;
    private String phoneNumber;
     // getters and setters
}

List<Person> bookList = new ArrayList<>();
bookList.add(new Person("jack","18163138123"));
bookList.add(new Person("martin",null));
// 空指针异常
bookList.stream().collect(Collectors.toMap(Person::getName, Person::getPhoneNumber));
```

下面我们来解释一下原因。

首先，我们来看 `java.util.stream.Collectors` 类的 `toMap()` 方法 ，可以看到其内部调用了 `Map` 接口的 `merge()` 方法。

```java
public static <T, K, U, M extends Map<K, U>>
Collector<T, ?, M> toMap(Function<? super T, ? extends K> keyMapper,
                            Function<? super T, ? extends U> valueMapper,
                            BinaryOperator<U> mergeFunction,
                            Supplier<M> mapSupplier) {
    BiConsumer<M, T> accumulator
            = (map, element) -> map.merge(keyMapper.apply(element),
                                          valueMapper.apply(element), mergeFunction);
    return new CollectorImpl<>(mapSupplier, accumulator, mapMerger(mergeFunction), CH_ID);
}
```

`Map` 接口的 `merge()` 方法如下，这个方法是接口中的默认实现。

> 如果你还不了解 Java 8 新特性的话，请看这篇文章：[《Java8 新特性总结》](../06_新特性/References/我，一个10年老程序员，最近才开始用 Java8 新特性.html) 。

```java
default V merge(K key, V value,
        BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
    Objects.requireNonNull(remappingFunction);
    Objects.requireNonNull(value);
    V oldValue = get(key);
    V newValue = (oldValue == null) ? value :
               remappingFunction.apply(oldValue, value);
    if(newValue == null) {
        remove(key);
    } else {
        put(key, newValue);
    }
    return newValue;
}
```

`merge()` 方法会先调用 `Objects.requireNonNull()` 方法判断 value 是否为空。

```java
public static <T> T requireNonNull(T obj) {
    if (obj == null)
        throw new NullPointerException();
    return obj;
}
```



⚠如果流中存在**相同 key** 的元素，只定义`Key` 和 `Value` 则会抛出 `IllegalStateException`。

当流中有重复 key 时，程序不知道该如何把重复的key对应的这些 value 合并，默认会报错。

为了处理这种情况，`toMap()` 提供了第三个参数 `mergeFunction`，用来告诉收集器：**如果遇到重复的 key，该怎么合并 value**。

💡 **重复 key 示例**

场景：统计一个字符串列表中每个首字母出现的次数

```java
List<String> list = Arrays.asList("apple", "apricot", "banana", "blueberry", "cherry");
```

使用 mergeFunction 合并 value（比如把所有相同首字母的字符串拼接起来）：

```java
Map<Character, String> map = list.stream()
    .collect(Collectors.toMap(
        s -> s.charAt(0),          // key: 首字母
        s -> s,                // value: 原字符串
        (v1, v2) -> v1 + "," + v2    // mergeFunction: 拼接
    ));

System.out.println(map);
```

**结果**：

```
{a=apple,apricot, b=banana,blueberry, c=cherry}
```

`Collectors`也提供了无需mergeFunction的`toMap()`方法，但此时若出现key冲突，则会抛出`IllegalStateException`异常，因此强烈建议使用`toMap()`方法必填 `mergeFunction` 。



**✅ 不同参数的 `toMap()` 总结** 

| 写法                                                        | 是否允许重复 key | 是否需要 mergeFunction | 是否可自定义 Map 类型 | 说明                                                         |
| ----------------------------------------------------------- | ---------------- | ---------------------- | --------------------- | ------------------------------------------------------------ |
| `toMap(keyMapper, valueMapper)`                             | ❌ 不允许         | ❌ 不需要               | ❌ 不可自定义          | 若 key 重复会抛出 `IllegalStateException`                    |
| `toMap(keyMapper, valueMapper, mergeFunction)`              | ✅ 允许           | ✅ 必须提供             | ❌ 不可自定义          | 使用 mergeFunction 合并冲突的 value，默认生成 `HashMap`      |
| `toMap(keyMapper, valueMapper, mergeFunction, mapSupplier)` | ✅ 允许           | ✅ 必须提供             | ✅ 可自定义            | 除合并外，还可以指定 Map 实现，如 `LinkedHashMap`, `TreeMap` 等 |

✅ **示例代码对比**

| 写法                              | 示例                                                         |
| --------------------------------- | ------------------------------------------------------------ |
| `toMap(k, v)`                     | `.collect(toMap(s -> s, s -> s.length()))`                   |
| `toMap(k, v, merge)`              | `.collect(toMap(s -> s.charAt(0), s -> s, (v1, v2) -> v1 + "," + v2))` |
| `toMap(k, v, merge, mapSupplier)` | `.collect(toMap(s -> s.charAt(0), s -> s, (v1, v2) -> v1 + "," + v2, LinkedHashMap::new))` |



## 3、集合遍历

《阿里巴巴 Java 开发手册》的描述如下：

> **不要在 foreach 循环里进行元素的 `remove/add` 操作。remove 元素请使用 `Iterator` 方式，如果并发操作，需要对 `Iterator` 对象加锁。**

通过反编译你会发现 foreach 语法底层其实还是依赖 `Iterator` 。不过， `remove/add` 操作直接调用的是集合自己的方法，而不是 `Iterator` 的 `remove/add`方法

这就导致 `Iterator` 莫名其妙地发现自己有元素被 `remove/add` ，然后，它就会抛出一个 `ConcurrentModificationException` 来提示用户发生了并发修改异常。这就是单线程状态下产生的 **fail-fast 机制**。

> **fail-fast 机制**：多个线程对 fail-fast 集合进行修改的时候，可能会抛出`ConcurrentModificationException`。 即使是单线程下也有可能会出现这种情况，上面已经提到过。
>
> 相关阅读：[什么是 fail-fast](./References/fail-fast/什么是fail-fast - 程序员自由之路 - 博客园.html) 。

Java8 开始，可以使用 `Collection#removeIf()`方法删除满足特定条件的元素，如

```java
List<Integer> list = new ArrayList<>();
for (int i = 1; i <= 10; ++i) {
    list.add(i);
}
list.removeIf(filter -> filter % 2 == 0); /* 删除list中的所有偶数 */
System.out.println(list); /* [1, 3, 5, 7, 9] */
```

⚠️**注意：`removeIf()` 并不是线程安全的**

- `removeIf()` 其实就是对集合做遍历，然后对符合条件的元素调用 `remove()`，**底层并没有任何同步或加锁机制**。

- 如果在一个线程里调用 `removeIf()`，同时另一个线程对这个集合进行修改（比如 add 或 remove），就会抛出 `ConcurrentModificationException`，或者导致数据不一致。

- 只有线程安全的集合调用 `removeIf()` 才是线程安全的，例如，使用 `CopyOnWriteArrayList`

	

除了上面介绍的直接使用 `Iterator` 进行遍历操作之外，你还可以：

* 使用普通的 for 循环
* 使用 fail-safe 的集合类。`java.util`包下面的所有的集合类都是 fail-fast 的，而`java.util.concurrent`包下面的所有的类都是 fail-safe 的。
* ……

### 3.1、集合安全遍历方案

#### 🌟 方案 1：**加同步锁（同步块）**

最通用、简单的做法：给集合加锁，保证遍历和修改互斥。

```java
List<String> list = new ArrayList<>();

synchronized (list) {
    Iterator<String> it = list.iterator();
    while (it.hasNext()) {
        String s = it.next();
        // 处理 s
    }
}
```

 ✅**优点**：写法简单
 ❌**缺点**：性能差（整个集合被锁住）

#### 🌟 方案 2：**使用并发安全集合**

- `CopyOnWriteArrayList`

	遍历时不会抛 `ConcurrentModificationException`，因为它底层是「写时复制」：遍历使用快照，写时复制新数组。

	```java
	CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
	for (String s : list) {
	    // 安全遍历
	}
	```

	✅ **适用场景**：读多写少。
	❌ **缺点**：写性能差，每次写都会复制整个数组。

-  `ConcurrentHashMap`

	遍历时安全，可以边遍历边修改（不会抛异常），但遍历时看到的数据是**弱一致性**（不保证你遍历到的就是最新数据）。

	```java
	ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
	for (Map.Entry<String, Integer> entry : map.entrySet()) {
	    // 安全遍历
	}
	```

	 ✅**优点**：读写都快
	 ❌**缺点**：数据弱一致性，不保证遍历到最新数据

#### 🌟 方案 3：**使用快照（复制集合）**

如果不想加锁，也不想用特殊集合，可以先复制一份快照再遍历。

```java
List<String> snapshot;
synchronized (list) {
    snapshot = new ArrayList<>(list);
}

for (String s : snapshot) {
    // 遍历快照，安全
}
```

✅**适用场景**：对一致性要求高时，或者需要遍历稳定内容。

#### 🌟 方案 4：使用 Collections.synchronizedList() 或其他同步包装

在 `java.util.Collections` 里，常用的同步包装方法有 👇

| 方法                                      | 描述                              |
| ----------------------------------------- | --------------------------------- |
| `synchronizedList(List<T> list)`          | 返回线程安全的 List               |
| `synchronizedMap(Map<K,V> m)`             | 返回线程安全的 Map                |
| `synchronizedSet(Set<T> s)`               | 返回线程安全的 Set                |
| `synchronizedSortedMap(SortedMap<K,V> m)` | 返回线程安全的 SortedMap          |
| `synchronizedSortedSet(SortedSet<T> s)`   | 返回线程安全的 SortedSet          |
| `synchronizedCollection(Collection<T> c)` | 返回线程安全的 Collection（通用） |

```java
List<String> list = Collections.synchronizedList(new ArrayList<>());

synchronized (list) {
    for (String s : list) {
        // 安全遍历
    }
}
```

⚠️ 注意：**使用 `synchronizedList` 后，遍历仍然需要显示加锁**，否则同样有并发风险。

#### 🟢 总结

| 方案                   | 是否需要加锁 | 遍历时是否允许修改 | 性能特点           | 适用场景         |
| ---------------------- | ------------ | ------------------ | ------------------ | ---------------- |
| 同步块（synchronized） | ✅            | ❌                  | 性能差             | 不常用，逻辑简单 |
| CopyOnWriteArrayList   | ❌            | ✅                  | 读快，写慢         | 读多写少         |
| ConcurrentHashMap      | ❌            | ✅                  | 读写都快           | 高并发           |
| 快照遍历（复制一份）   | ❌            | ✅（快照可）        | 性能取决于复制开销 | 严格一致性       |
| synchronizedXxx 包装类 | ✅            | ❌                  | 性能差             | 低并发           |

💬 **最常用推荐**

 ✅ 如果是 **读多写少**，用 `CopyOnWriteArrayList`
 ✅ 如果是 **读写都多**，用 `ConcurrentHashMap`
 ✅ 如果要严格一致性，复制快照

## 4、集合去重

《阿里巴巴 Java 开发手册》的描述如下：

> **可以利用 `Set` 元素唯一的特性，可以快速对一个集合进行去重操作，避免使用 `List` 的 `contains()` 进行遍历去重或者判断包含操作。**

这里我们以 `HashSet` 和 `ArrayList` 为例说明。

```java
// Set 去重代码示例
public static <T> Set<T> removeDuplicateBySet(List<T> data) {

    // 整体时间复杂度为：O(n)
    if (CollectionUtils.isEmpty(data)) {
        // 每次 containsKey() 的时间复杂度为 O(1)
        return new HashSet<>();
    }
    return new HashSet<>(data);
}

// List 去重代码示例
public static <T> List<T> removeDuplicateByList(List<T> data) {

    if (CollectionUtils.isEmpty(data)) {
        return new ArrayList<>();

    }
    List<T> result = new ArrayList<>(data.size());
    // 整体时间复杂度为O(n²)
    // 1、for循环时间复杂度为O(n)
    for (T current : data) {
        // 2、contains() 每次的时间复杂度为O(n)
        if (!result.contains(current)) {
            result.add(current);
        }
    }
    return result;
}
```

两者的核心差别在于 `contains()` 方法的实现。

`HashSet` 的 `contains()` 方法底部依赖的 `HashMap` 的 `containsKey()` 方法，时间复杂度接近于 O（1）（没有出现哈希冲突的时候为 O（1））。

```java
private transient HashMap<E,Object> map;
public boolean contains(Object o) {
    return map.containsKey(o);
}
```

我们有 N 个元素插入进 Set 中，那时间复杂度就接近是 **O (n)**。

`ArrayList` 的 `contains()` 方法是通过遍历所有元素的方法来做的，`contains()` 方法的时间复杂度接近是 O(n)，。

```java
public boolean contains(Object o) {
    return indexOf(o) >= 0;
}
public int indexOf(Object o) {
    if (o == null) {
        for (int i = 0; i < size; i++)
            if (elementData[i]==null)
                return i;
    } else {
        for (int i = 0; i < size; i++)
            if (o.equals(elementData[i]))
                return i;
    }
    return -1;
}
```

✅ **总结对比表**

| 方法              | 是否遍历链表 | 单次查找复杂度 | 总体复杂度（n 个元素） | 备注           |
| ----------------- | ------------ | -------------- | ---------------------- | -------------- |
| List + contains() | ✅ 全遍历     | O(n)           | O(n²)                  | 慢，常数因子大 |
| HashSet           | ❌ 基于 hash  | O(1) 平均      | O(n)                   | 快，常数因子小 |
| TreeSet           | ❌ 基于红黑树 | O(log n)       | O(n log n)             | 自动排序       |

## 5、集合转数组

《阿里巴巴 Java 开发手册》的描述如下：

> **使用集合转数组的方法，必须使用集合的 `toArray(T[] array)`，传入的是类型完全一致、长度为 0 的空数组。**

`toArray(T[] array)` 方法的参数是一个泛型数组，如果 `toArray` 方法中没有传递任何参数的话返回的是 `Object`类 型数组。

```java
String [] s= new String[]{
    "dog", "lazy", "a", "over", "jumps", "fox", "brown", "quick", "A"
};
List<String> list = Arrays.asList(s);
Collections.reverse(list);
//没有指定类型的话会报错
s=list.toArray(new String[0]);
```

由于 JVM 优化，`new String[0]`作为`Collection.toArray()`方法的参数现在使用更好，`new String[0]`就是起一个模板的作用，指定了返回数组的类型，0 是为了节省空间，因为它只是为了说明返回的类型。详见：https://shipilev.net/blog/2016/arrays-wisdom-ancients

## 6、数组转集合

《阿里巴巴 Java 开发手册》的描述如下：

> **使用工具类 `Arrays.asList()` 把数组转换成集合时，不能使用其修改集合相关的方法， 它的 `add/remove/clear` 方法会抛出 `UnsupportedOperationException` 异常。**

我在之前的一个项目中就遇到一个类似的坑。

`Arrays.asList()`在平时开发中还是比较常见的，我们可以使用它将一个数组转换为一个 `List` 集合。

```java
String[] myArray = {"Apple", "Banana", "Orange"};
List<String> myList = Arrays.asList(myArray);
//上面两个语句等价于下面一条语句
List<String> myList = Arrays.asList("Apple","Banana", "Orange");
```

JDK 源码对于这个方法的说明：

`Arrays.asList()` 返回的 List 并不是 `java.util.ArrayList`，而是 `Arrays` 的一个内部静态类：

```java
/**
  *返回由指定数组支持的固定大小的列表。此方法作为基于数组和基于集合的API之间的桥梁，
  * 与 Collection.toArray()结合使用。返回的List是可序列化并实现RandomAccess接口。
  */
public static <T> List<T> asList(T... a) {
    return new ArrayList<>(a);
}
```

⚠️ 注意，这个 `ArrayList` 是 `Arrays` 内部定义的内部静态类，并不是 `java.util.ArrayList`！

这个内部类直接持有原数组的引用，大小不能变动，只能修改已有元素的值（即 set(index, element) 是允许的）。



下面我们来总结一下使用注意事项。

**1、`Arrays.asList()`是泛型方法，传递的数组必须是对象数组，而不是基本类型。**

```java
int[] myArray = {1, 2, 3};
List myList = Arrays.asList(myArray);
System.out.println(myList.size());//1
System.out.println(myList.get(0));//数组地址值
System.out.println(myList.get(1));//报错：ArrayIndexOutOfBoundsException
int[] array = (int[]) myList.get(0);
System.out.println(array[0]);//1
```

当传入一个**原生数据类型数组**时，`Arrays.asList()` 真正得到的参数就不是数组中的元素，而是数组对象本身！此时 `List` 的唯一元素就是这个数组，这也就解释了上面的代码。

我们使用包装类型数组就可以解决这个问题。

```java
Integer[] myArray = {1, 2, 3};
```

**2、使用集合的修改方法: `add()`、`remove()`、`clear()`会抛出异常。**

```java
List myList = Arrays.asList(1, 2, 3);
myList.add(4);//运行时报错：UnsupportedOperationException
myList.remove(1);//运行时报错：UnsupportedOperationException
myList.clear();//运行时报错：UnsupportedOperationException
```

`Arrays.asList()` 方法返回的并不是 `java.util.ArrayList` ，而是 `java.util.Arrays` 的一个内部类,这个内部类并没有实现集合的修改方法或者说并没有重写这些方法。

```java
List myList = Arrays.asList(1, 2, 3);
System.out.println(myList.getClass());//class java.util.Arrays$ArrayList
```

下图是 `java.util.Arrays$ArrayList` 的简易源码，我们可以看到这个类重写的方法有哪些。

```java
  private static class ArrayList<E> extends AbstractList<E>
        implements RandomAccess, java.io.Serializable
    {
        ...

        @Override
        public E get(int index) {
          ...
        }

        @Override
        public E set(int index, E element) {
          ...
        }

        @Override
        public int indexOf(Object o) {
          ...
        }

        @Override
        public boolean contains(Object o) {
           ...
        }

        @Override
        public void forEach(Consumer<? super E> action) {
          ...
        }

        @Override
        public void replaceAll(UnaryOperator<E> operator) {
          ...
        }

        @Override
        public void sort(Comparator<? super E> c) {
          ...
        }
    }
```

我们再看一下`java.util.AbstractList`的 `add/remove/clear` 方法就知道为什么会抛出 `UnsupportedOperationException` 了。

```java
public E remove(int index) {
    throw new UnsupportedOperationException();
}
public boolean add(E e) {
    add(size(), e);
    return true;
}
public void add(int index, E element) {
    throw new UnsupportedOperationException();
}

public void clear() {
    removeRange(0, size());
}
protected void removeRange(int fromIndex, int toIndex) {
    ListIterator<E> it = listIterator(fromIndex);
    for (int i=0, n=toIndex-fromIndex; i<n; i++) {
        it.next();
        it.remove();
    }
}
```

**那我们如何正确的将数组转换为 `ArrayList` ?**

1、手动实现工具类

```java
//JDK1.5+
static <T> List<T> arrayToList(final T[] array) {
  final List<T> l = new ArrayList<T>(array.length);

  for (final T s : array) {
    l.add(s);
  }
  return l;
}


Integer [] myArray = { 1, 2, 3 };
System.out.println(arrayToList(myArray).getClass());//class java.util.ArrayList
```

2、最简便的方法

```java
List list = new ArrayList<>(Arrays.asList("a", "b", "c"))
```

3、使用 Java8 的 `Stream`(推荐)

```java
Integer [] myArray = { 1, 2, 3 };
List myList = Arrays.stream(myArray).collect(Collectors.toList());

//基本类型也可以实现转换（依赖boxed的装箱操作）
int [] myArray2 = { 1, 2, 3 };
List myList = Arrays.stream(myArray2).boxed().collect(Collectors.toList());
```

4、使用 Guava

对于**不可变**集合，你可以使用[`ImmutableList`](https://github.com/google/guava/blob/master/guava/src/com/google/common/collect/ImmutableList.java)类及其[`of()`](https://github.com/google/guava/blob/master/guava/src/com/google/common/collect/ImmutableList.java#L101)与[`copyOf()`](https://github.com/google/guava/blob/master/guava/src/com/google/common/collect/ImmutableList.java#L225)工厂方法：（参数不能为空）

```java
List<String> il = ImmutableList.of("string", "elements");  // from varargs
List<String> il = ImmutableList.copyOf(aStringArray);      // from array
```

对于**可变**集合，你可以使用[`Lists`](https://github.com/google/guava/blob/master/guava/src/com/google/common/collect/Lists.java)类及其[`newArrayList()`](https://github.com/google/guava/blob/master/guava/src/com/google/common/collect/Lists.java#L87)工厂方法：

```java
List<String> l1 = Lists.newArrayList(anotherListOrCollection);    // from collection
List<String> l2 = Lists.newArrayList(aStringArray);               // from array
List<String> l3 = Lists.newArrayList("or", "string", "elements"); // from varargs
```

5、使用 Apache Commons Collections

```java
List<String> list = new ArrayList<String>();
CollectionUtils.addAll(list, str);
```

6、 使用 Java9 的 `List.of()`方法

```java
Integer[] array = {1, 2, 3};
List<Integer> list = List.of(array);
```



