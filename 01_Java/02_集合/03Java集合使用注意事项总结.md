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

Java8 开始，可以使用 `Collection#removeIf()`方法删除满足特定条件的元素,如

```java
List<Integer> list = new ArrayList<>();
for (int i = 1; i <= 10; ++i) {
    list.add(i);
}
list.removeIf(filter -> filter % 2 == 0); /* 删除list中的所有偶数 */
System.out.println(list); /* [1, 3, 5, 7, 9] */
```



除了上面介绍的直接使用 `Iterator` 进行遍历操作之外，你还可以：

* 使用普通的 for 循环
* 使用 fail-safe 的集合类。`java.util`包下面的所有的集合类都是 fail-fast 的，而`java.util.concurrent`包下面的所有的类都是 fail-safe 的。
* ……

