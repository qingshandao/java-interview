# 一、ArrayList 简介

`ArrayList` 的底层是数组队列，相当于动态数组。与 Java 中的数组相比，它的容量能动态增长。在添加大量元素前，应用程序可以使用`ensureCapacity`操作来增加 `ArrayList` 实例的容量。这可以减少递增式再分配的数量。

`ArrayList` 继承于 `AbstractList` ，实现了 `List`, `RandomAccess`, `Cloneable`, `java.io.Serializable` 这些接口。

```java
public class ArrayList<E> extends AbstractList<E>
        implements List<E>, RandomAccess, Cloneable, java.io.Serializable{

}
```

- `List` : 表明它是一个列表，支持添加、删除、查找等操作，并且可以通过下标进行访问。

- `RandomAccess` ：这是一个标志接口，表明实现这个接口的 `List` 集合是支持 **快速随机访问** 的。在 `ArrayList` 中，我们即可以通过元素的序号快速获取元素对象，这就是快速随机访问。

- `Cloneable` ：表明它具有拷贝能力，可以进行深拷贝或浅拷贝操作。

- `Serializable` : 表明它可以进行序列化操作，也就是可以将对象转换为字节流进行持久化存储或网络传输，非常方便。

![](../assets/arraylist-class-diagram.png)

## 1.1、ArrayList 和 Vector 的区别?（了解即可）

- `ArrayList` 是 `List` 的主要实现类，底层使用 `Object[]`存储，适用于频繁的查找工作，线程不安全 。

- `Vector` 是 `List` 的古老实现类，底层使用`Object[]` 存储，线程安全。

## 1.2、ArrayList 可以添加 null 值吗？

可以存储任何类型的对象，包括 `null` 值。

不过，不建议向`ArrayList` 中添加 `null` 值， `null` 值无意义，会让代码难以维护比如忘记做判空处理就会导致空指针异常。

示例代码：

```java
ArrayList<String> listOfStrings = new ArrayList<>();
listOfStrings.add(null);
listOfStrings.add("java");
System.out.println(listOfStrings);
```

输出：

```java
[null, java]
```

## 1.3、Arraylist 与 LinkedList 区别?

**是否保证线程安全：** `ArrayList` 和 `LinkedList` 都是**不同步**的，也就是不保证线程安全；

**底层数据结构：** 

- `ArrayList` 底层使用的是 **`Object` 数组**；
- `LinkedList` 底层使用的是 **双向链表** 数据结构（JDK1.6 之前为循环链表，JDK1.7 取消了循环。注意双向链表和双向循环链表的区别，下面有介绍到！）

**插入和删除是否受元素位置的影响：**

* `ArrayList` 采用数组存储，所以插入和删除元素的时间复杂度受元素位置的影响。 比如：执行`add(E e)`方法的时候， `ArrayList` 会默认在将指定的元素追加到此列表的末尾，这种情况时间复杂度就是 O(1)。但是如果要在指定位置 i 插入和删除元素的话（`add(int index, E element)`），时间复杂度就为 O(n)。因为在进行上述操作的时候集合中第 i 和第 i 个元素之后的(n-i)个元素都要执行向后位/向前移一位的操作。
* `LinkedList` 采用链表存储，所以在头尾插入或者删除元素不受元素位置的影响（`add(E e)`、`addFirst(E e)`、`addLast(E e)`、`removeFirst()`、 `removeLast()`），时间复杂度为 O(1)，如果是要在指定位置 `i` 插入和删除元素的话（`add(int index, E element)`，`remove(Object o)`,`remove(int index)`）， 时间复杂度为 O(n) ，因为需要先移动到指定位置再插入和删除。

* **是否支持快速随机访问：** `LinkedList` 不支持高效的随机元素访问，而 `ArrayList`（实现了 `RandomAccess` 接口） 支持。快速随机访问就是通过元素的序号快速获取元素对象(对应于`get(int index)`方法)。
* **内存空间占用：** `ArrayList` 的空间浪费主要体现在在 list 列表的结尾会预留一定的容量空间，而 LinkedList 的空间花费则体现在它的每一个元素都需要消耗比 ArrayList 更多的空间（因为要存放直接后继和直接前驱以及数据）。

## 1.4、ArrayList 核心源码解读

这里以 JDK1.8 为例，分析一下 `ArrayList` 的底层源码。

### 1、类定义

```java
public class ArrayList<E> extends AbstractList<E>
        implements List<E>, RandomAccess, Cloneable, java.io.Serializable {
    
    private static final long serialVersionUID = 8683452581122892189L;
```



### 2、默认初始容量大小

```java
	/**
     * 默认初始容量大小
     */
    private static final int DEFAULT_CAPACITY = 10;
```



### 3、空数组

```java
	/**
     * 空数组（用于空实例）。
     */
    private static final Object[] EMPTY_ELEMENTDATA = {};
```



### 4、共享空数组

```java
//用于默认大小空实例的共享空数组实例。
//我们把它从EMPTY_ELEMENTDATA数组中区分出来，以知道在添加第一个元素时容量需要增加多少。
private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};
```



### 5、存ArrayList数据的数组

```java
	/**
     * 保存ArrayList数据的数组
     */
    transient Object[] elementData; // non-private to simplify nested class access
```



### 6、ArrayList 实际所包含的元素个数

```java
	/**
     * ArrayList 实际所包含的元素个数
     */
    private int size;
```



### 7、构造函数 —— 带初始容量

```java
	/**
     * 带初始容量参数的构造函数（用户可以在创建ArrayList对象时自己指定集合的初始大小）
     */
    public ArrayList(int initialCapacity) {
        if (initialCapacity > 0) {
            //如果传入的参数大于0，创建initialCapacity大小的数组
            this.elementData = new Object[initialCapacity];
        } else if (initialCapacity == 0) {
            //如果传入的参数等于0，创建空数组
            this.elementData = EMPTY_ELEMENTDATA;
        } else {
            //其他情况，抛出异常
            throw new IllegalArgumentException("Illegal Capacity: " +
                    initialCapacity);
        }
    }
```



### 8、构造函数 —— 无参

```java
	/**
     * 默认无参构造函数
     * DEFAULTCAPACITY_EMPTY_ELEMENTDATA 为0.初始化为10，也就是说初始其实是空数组 当添加第一个元素的时候数组容量才变成10
     */
    public ArrayList() {
        this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
    }
```



### 9、构造函数 —— 指定集合

```java
	/**
     * 构造一个包含指定集合的元素的列表，按照它们由集合的迭代器返回的顺序。
     */
    public ArrayList(Collection<? extends E> c) {
        //将指定集合转换为数组
        elementData = c.toArray();
        //如果elementData数组的长度不为0
        if ((size = elementData.length) != 0) {
            // 如果elementData不是Object类型数据（c.toArray可能返回的不是Object类型的数组所以加上下面的语句用于判断）
            if (elementData.getClass() != Object[].class)
                //将原来不是Object类型的elementData数组的内容，赋值给新的Object类型的elementData数组
                elementData = Arrays.copyOf(elementData, size, Object[].class);
        } else {
            // 其他情况，用空数组代替
            this.elementData = EMPTY_ELEMENTDATA;
        }
    }
```



### 10、最小化ArrayList实例的容量

`trimToSize()` 方法是 `ArrayList` 提供的一个 **容量优化方法**，它的作用是把底层数组 `elementData` 的容量收缩到刚好和当前元素个数 `size` 相同，来节省内存空间。

```java
public void trimToSize() {
    modCount++;
    if (size < elementData.length) {
        elementData = (size == 0)
                ? EMPTY_ELEMENTDATA
                : Arrays.copyOf(elementData, size);
    }
}
```

> ① `modCount++`
>
> ```java
> modCount++;
> ```
>
> * `modCount` 是 `ArrayList` 从 `AbstractList` 继承来的字段，用于记录结构性修改次数。
> * 当集合结构发生变化（比如改变容量、添加、删除元素等），`modCount` 会加 1。
> * 这个变量主要用于快速失败（fail-fast）机制，比如在遍历时如果有其他线程修改，会抛出 `ConcurrentModificationException`。
>
> ------
>
> ② 判断是否需要收缩
>
> ```java
> if (size < elementData.length) {
> ```
>
> * 判断当前实际元素个数 `size` 是否小于数组容量。
> * 如果相等，说明数组已经刚好合适，无需操作。
>
> ------
>
> ③ 收缩数组
> ```java
> elementData = (size == 0)
>     ? EMPTY_ELEMENTDATA
>     : Arrays.copyOf(elementData, size);
> ```
>
> **两种情况：**
>
> ✅ 情况 1：当前没有任何元素（`size == 0`）
>
> ```java
> elementData = EMPTY_ELEMENTDATA;
> ```
>
> * 直接将底层数组指向一个共享的空数组 `EMPTY_ELEMENTDATA`，这样可以避免占用内存。
>
> ✅ 情况 2：当前有元素（`size > 0`）
>
> ```java
> elementData = Arrays.copyOf(elementData, size);
> ```
>
> * 调用 `Arrays.copyOf()`，将原来的数组复制一份，长度只保留到 `size`，丢弃后面多余的容量。
>
> 
>
> **💭 为什么需要 `trimToSize()`？**
>
> `ArrayList` 默认会预留容量（比如扩容时会按 1.5 倍左右增长），在大量元素删除后，底层数组依然保持原来的容量，浪费内存。如果后面不再添加元素，就可以调用 `trimToSize()` 来节省空间。



### 11、扩容机制

确保底层数组容量至少能容纳 `minCapacity` 个元素。如果不够，就会扩容。

```java
public void ensureCapacity(int minCapacity) {
    int minExpand = (elementData != DEFAULTCAPACITY_EMPTY_ELEMENTDATA)
            ? 0
            : DEFAULT_CAPACITY;

    if (minCapacity > minExpand) {
        ensureExplicitCapacity(minCapacity);
    }
}
```

> ① 定义 `minExpand`
>
> ```java
> int minExpand = (elementData != DEFAULTCAPACITY_EMPTY_ELEMENTDATA)
>         ? 0
>         : DEFAULT_CAPACITY;
> ```
>
> 🔎 这里的逻辑含义：
>
> * `elementData != DEFAULTCAPACITY_EMPTY_ELEMENTDATA`
> 	* 如果当前底层数组不是默认空数组（也就是说已经有过容量分配或者非空初始化过），则 `minExpand = 0`，表示可以按实际需要的最小容量来扩容。
> * 否则（还处于默认空数组状态），`minExpand = DEFAULT_CAPACITY`（即 10）。因为 ArrayList 默认首次分配时，容量最小为 10。
>
> ✅ 总结：
>
> | 情况           | minExpand 的值 |
> | -------------- | -------------- |
> | 不是默认空数组 | 0              |
> | 是默认空数组   | 10             |

> ② 判断是否需要扩容
>
> ```java
> if (minCapacity > minExpand) {
>     ensureExplicitCapacity(minCapacity);
> }
> ```
>
> 这里检查传入的 `minCapacity` 是否大于 `minExpand`。
>
> * 如果大于，表示需要扩容，就调用 `ensureExplicitCapacity(minCapacity)`.
> * 如果不大于，则不做任何操作（容量足够）。

> ③ 调用 `ensureExplicitCapacity`
>
> ```java
> private void ensureExplicitCapacity(int minCapacity) {
>     modCount++;
> 
>     // overflow-conscious code
>     if (minCapacity - elementData.length > 0)
>         grow(minCapacity);
> }
> ```
>
> **解释：**
>
> * `modCount++`：记录结构性修改（用于 fail-fast）。
> * 判断 `minCapacity - elementData.length > 0`
> 	* 如果成立，表示 `minCapacity` 超过当前底层数组容量，必须扩容，进入 `grow(minCapacity)` 方法。

> ④ `grow()` 方法（核心）
>
> ```java
> private void grow(int minCapacity) {
>     int oldCapacity = elementData.length;
>     int newCapacity = oldCapacity + (oldCapacity >> 1); // 1.5倍扩容
> 
>     if (newCapacity - minCapacity < 0)
>         newCapacity = minCapacity;
> 
>     if (newCapacity - MAX_ARRAY_SIZE > 0)
>         newCapacity = hugeCapacity(minCapacity);
> 
>     elementData = Arrays.copyOf(elementData, newCapacity);
> }
> ```
>
> **核心逻辑：**
>
> * 默认扩容为原容量的 1.5 倍：`oldCapacity + (oldCapacity >> 1)`
> * 如果 1.5 倍还不够，就直接使用 `minCapacity`。
> * 如果超过 `MAX_ARRAY_SIZE`（大约是 `Integer.MAX_VALUE - 8`），做安全处理。

> **🟡 为什么需要 `minExpand`？**
>
> 因为当 `ArrayList` 刚被创建时，如果你没有添加元素，它内部的数组实际上是一个空的共享空数组（`DEFAULTCAPACITY_EMPTY_ELEMENTDATA`），**为了节省内存，不会立即分配 10 个空间**。
>
> 只有在首次添加元素或调用 `ensureCapacity()` 且 `minCapacity > 10` 时，才会正式分配数组并初始化容量。



### 12、计算所需容量

用于在确定需要新容量时，计算应该分配的实际容量，它主要在「首次分配底层数组」时使用，保证默认情况下容量不会太小（默认至少 10）。

```java
private static int calculateCapacity(Object[] elementData, int minCapacity) {
    if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
        return Math.max(DEFAULT_CAPACITY, minCapacity);
    }
    return minCapacity;
}
```



### 13、确保内部容量达到指定最小容量

当我们向 `ArrayList` 添加元素（比如 `add()`）时，都会先调用 `ensureCapacityInternal()`，以确保底层数组足够大。

```java
private void ensureCapacityInternal(int minCapacity) {
    ensureExplicitCapacity(calculateCapacity(elementData, minCapacity));
}
```

> ✅ 参数 `minCapacity`
>
> * 表示：**当前操作需要的最小容量**（比如当前 size + 1）

```java
private static int calculateCapacity(Object[] elementData, int minCapacity) {
    if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
        return Math.max(DEFAULT_CAPACITY, minCapacity);
    }
    return minCapacity;
}
```

> ✅  含义总结：
>
> * 如果数组还没初始化（`elementData` 是默认空数组），则返回 `max(10, minCapacity)`，保证初次容量至少为 10。
> * 如果数组已经初始化，直接返回 `minCapacity`。
>
> 👉 **此处主要解决初始容量问题**



### 14、判断是否需要扩容

```java
private void ensureExplicitCapacity(int minCapacity) {
    modCount++;
    if (minCapacity - elementData.length > 0)
        grow(minCapacity);
}
```

>  ✅ 修改 `modCount`
>
> * `modCount++`：结构性修改计数（用于 fail-fast，比如遍历时检测并发修改）。
>
> ✅ 判断是否需要扩容
>
> ```java
> if (minCapacity - elementData.length > 0)
>     grow(minCapacity);
> ```
>
> 含义：
>
> * 如果 `minCapacity > elementData.length`，就需要扩容。
> * 否则容量足够，不需要操作。





```java
    /**
     * 要分配的最大数组大小
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * ArrayList扩容的核心方法。
     */
    private void grow(int minCapacity) {
        // oldCapacity为旧容量，newCapacity为新容量
        int oldCapacity = elementData.length;
        //将oldCapacity 右移一位，其效果相当于oldCapacity /2，
        //我们知道位运算的速度远远快于整除运算，整句运算式的结果就是将新容量更新为旧容量的1.5倍，
        int newCapacity = oldCapacity + (oldCapacity >> 1);
        //然后检查新容量是否大于最小需要容量，若还是小于最小需要容量，那么就把最小需要容量当作数组的新容量，
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        //再检查新容量是否超出了ArrayList所定义的最大容量，
        //若超出了，则调用hugeCapacity()来比较minCapacity和 MAX_ARRAY_SIZE，
        //如果minCapacity大于MAX_ARRAY_SIZE，则新容量则为Integer.MAX_VALUE，否则，新容量大小则为 MAX_ARRAY_SIZE。
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);
        // minCapacity is usually close to size, so this is a win:
        elementData = Arrays.copyOf(elementData, newCapacity);
    }

    //比较minCapacity和 MAX_ARRAY_SIZE
    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError();
        return (minCapacity > MAX_ARRAY_SIZE) ?
                Integer.MAX_VALUE :
                MAX_ARRAY_SIZE;
    }

    /**
     * 返回此列表中的元素数。
     */
    public int size() {
        return size;
    }

    /**
     * 如果此列表不包含元素，则返回 true 。
     */
    public boolean isEmpty() {
        //注意=和==的区别
        return size == 0;
    }

    /**
     * 如果此列表包含指定的元素，则返回true 。
     */
    public boolean contains(Object o) {
        //indexOf()方法：返回此列表中指定元素的首次出现的索引，如果此列表不包含此元素，则为-1
        return indexOf(o) >= 0;
    }

    /**
     * 返回此列表中指定元素的首次出现的索引，如果此列表不包含此元素，则为-1
     */
    public int indexOf(Object o) {
        if (o == null) {
            for (int i = 0; i < size; i++)
                if (elementData[i] == null)
                    return i;
        } else {
            for (int i = 0; i < size; i++)
                //equals()方法比较
                if (o.equals(elementData[i]))
                    return i;
        }
        return -1;
    }

    /**
     * 返回此列表中指定元素的最后一次出现的索引，如果此列表不包含元素，则返回-1。.
     */
    public int lastIndexOf(Object o) {
        if (o == null) {
            for (int i = size - 1; i >= 0; i--)
                if (elementData[i] == null)
                    return i;
        } else {
            for (int i = size - 1; i >= 0; i--)
                if (o.equals(elementData[i]))
                    return i;
        }
        return -1;
    }

    /**
     * 返回此ArrayList实例的浅拷贝。 （元素本身不被复制。）
     */
    public Object clone() {
        try {
            ArrayList<?> v = (ArrayList<?>) super.clone();
            //Arrays.copyOf功能是实现数组的复制，返回复制后的数组。参数是被复制的数组和复制的长度
            v.elementData = Arrays.copyOf(elementData, size);
            v.modCount = 0;
            return v;
        } catch (CloneNotSupportedException e) {
            // 这不应该发生，因为我们是可以克隆的
            throw new InternalError(e);
        }
    }

    /**
     * 以正确的顺序（从第一个到最后一个元素）返回一个包含此列表中所有元素的数组。
     * 返回的数组将是“安全的”，因为该列表不保留对它的引用。
     * （换句话说，这个方法必须分配一个新的数组）。
     * 因此，调用者可以自由地修改返回的数组结构。
     * 注意：如果元素是引用类型，修改元素的内容会影响到原列表中的对象。
     * 此方法充当基于数组和基于集合的API之间的桥梁。
     */
    public Object[] toArray() {
        return Arrays.copyOf(elementData, size);
    }

    /**
     * 以正确的顺序返回一个包含此列表中所有元素的数组（从第一个到最后一个元素）;
     * 返回的数组的运行时类型是指定数组的运行时类型。 如果列表适合指定的数组，则返回其中。
     * 否则，将为指定数组的运行时类型和此列表的大小分配一个新数组。
     * 如果列表适用于指定的数组，其余空间（即数组的列表数量多于此元素），则紧跟在集合结束后的数组中的元素设置为null 。
     * （这仅在调用者知道列表不包含任何空元素的情况下才能确定列表的长度。）
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a.length < size)
            // 新建一个运行时类型的数组，但是ArrayList数组的内容
            return (T[]) Arrays.copyOf(elementData, size, a.getClass());
        //调用System提供的arraycopy()方法实现数组之间的复制
        System.arraycopy(elementData, 0, a, 0, size);
        if (a.length > size)
            a[size] = null;
        return a;
    }

    // Positional Access Operations

    @SuppressWarnings("unchecked")
    E elementData(int index) {
        return (E) elementData[index];
    }

    /**
     * 返回此列表中指定位置的元素。
     */
    public E get(int index) {
        rangeCheck(index);

        return elementData(index);
    }

    /**
     * 用指定的元素替换此列表中指定位置的元素。
     */
    public E set(int index, E element) {
        //对index进行界限检查
        rangeCheck(index);

        E oldValue = elementData(index);
        elementData[index] = element;
        //返回原来在这个位置的元素
        return oldValue;
    }

    /**
     * 将指定的元素追加到此列表的末尾。
     */
    public boolean add(E e) {
        ensureCapacityInternal(size + 1);  // Increments modCount!!
        //这里看到ArrayList添加元素的实质就相当于为数组赋值
        elementData[size++] = e;
        return true;
    }

    /**
     * 在此列表中的指定位置插入指定的元素。
     * 先调用 rangeCheckForAdd 对index进行界限检查；然后调用 ensureCapacityInternal 方法保证capacity足够大；
     * 再将从index开始之后的所有成员后移一个位置；将element插入index位置；最后size加1。
     */
    public void add(int index, E element) {
        rangeCheckForAdd(index);

        ensureCapacityInternal(size + 1);  // Increments modCount!!
        //arraycopy()这个实现数组之间复制的方法一定要看一下，下面就用到了arraycopy()方法实现数组自己复制自己
        System.arraycopy(elementData, index, elementData, index + 1,
                size - index);
        elementData[index] = element;
        size++;
    }

    /**
     * 删除该列表中指定位置的元素。 将任何后续元素移动到左侧（从其索引中减去一个元素）。
     */
    public E remove(int index) {
        rangeCheck(index);

        modCount++;
        E oldValue = elementData(index);

        int numMoved = size - index - 1;
        if (numMoved > 0)
            System.arraycopy(elementData, index + 1, elementData, index,
                    numMoved);
        elementData[--size] = null; // clear to let GC do its work
        //从列表中删除的元素
        return oldValue;
    }

    /**
     * 从列表中删除指定元素的第一个出现（如果存在）。 如果列表不包含该元素，则它不会更改。
     * 返回true，如果此列表包含指定的元素
     */
    public boolean remove(Object o) {
        if (o == null) {
            for (int index = 0; index < size; index++)
                if (elementData[index] == null) {
                    fastRemove(index);
                    return true;
                }
        } else {
            for (int index = 0; index < size; index++)
                if (o.equals(elementData[index])) {
                    fastRemove(index);
                    return true;
                }
        }
        return false;
    }

    /*
     * 该方法为私有的移除方法，跳过了边界检查，并且不返回被移除的值。
     */
    private void fastRemove(int index) {
        modCount++;
        int numMoved = size - index - 1;
        if (numMoved > 0)
            System.arraycopy(elementData, index + 1, elementData, index,
                    numMoved);
        elementData[--size] = null; // 在移除元素后，将该位置的元素设为 null，以便垃圾回收器（GC）能够回收该元素。
    }

    /**
     * 从列表中删除所有元素。
     */
    public void clear() {
        modCount++;

        // 把数组中所有的元素的值设为null
        for (int i = 0; i < size; i++)
            elementData[i] = null;

        size = 0;
    }

    /**
     * 按指定集合的Iterator返回的顺序将指定集合中的所有元素追加到此列表的末尾。
     */
    public boolean addAll(Collection<? extends E> c) {
        Object[] a = c.toArray();
        int numNew = a.length;
        ensureCapacityInternal(size + numNew);  // Increments modCount
        System.arraycopy(a, 0, elementData, size, numNew);
        size += numNew;
        return numNew != 0;
    }

    /**
     * 将指定集合中的所有元素插入到此列表中，从指定的位置开始。
     */
    public boolean addAll(int index, Collection<? extends E> c) {
        rangeCheckForAdd(index);

        Object[] a = c.toArray();
        int numNew = a.length;
        ensureCapacityInternal(size + numNew);  // Increments modCount

        int numMoved = size - index;
        if (numMoved > 0)
            System.arraycopy(elementData, index, elementData, index + numNew,
                    numMoved);

        System.arraycopy(a, 0, elementData, index, numNew);
        size += numNew;
        return numNew != 0;
    }

    /**
     * 从此列表中删除所有索引为fromIndex （含）和toIndex之间的元素。
     * 将任何后续元素移动到左侧（减少其索引）。
     */
    protected void removeRange(int fromIndex, int toIndex) {
        modCount++;
        int numMoved = size - toIndex;
        System.arraycopy(elementData, toIndex, elementData, fromIndex,
                numMoved);

        // clear to let GC do its work
        int newSize = size - (toIndex - fromIndex);
        for (int i = newSize; i < size; i++) {
            elementData[i] = null;
        }
        size = newSize;
    }

    /**
     * 检查给定的索引是否在范围内。
     */
    private void rangeCheck(int index) {
        if (index >= size)
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    /**
     * add和addAll使用的rangeCheck的一个版本
     */
    private void rangeCheckForAdd(int index) {
        if (index > size || index < 0)
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    /**
     * 返回IndexOutOfBoundsException细节信息
     */
    private String outOfBoundsMsg(int index) {
        return "Index: " + index + ", Size: " + size;
    }

    /**
     * 从此列表中删除指定集合中包含的所有元素。
     */
    public boolean removeAll(Collection<?> c) {
        Objects.requireNonNull(c);
        //如果此列表被修改则返回true
        return batchRemove(c, false);
    }

    /**
     * 仅保留此列表中包含在指定集合中的元素。
     * 换句话说，从此列表中删除其中不包含在指定集合中的所有元素。
     */
    public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return batchRemove(c, true);
    }


    /**
     * 从列表中的指定位置开始，返回列表中的元素（按正确顺序）的列表迭代器。
     * 指定的索引表示初始调用将返回的第一个元素为next 。 初始调用previous将返回指定索引减1的元素。
     * 返回的列表迭代器是fail-fast 。
     */
    public ListIterator<E> listIterator(int index) {
        if (index < 0 || index > size)
            throw new IndexOutOfBoundsException("Index: " + index);
        return new ListItr(index);
    }

    /**
     * 返回列表中的列表迭代器（按适当的顺序）。
     * 返回的列表迭代器是fail-fast 。
     */
    public ListIterator<E> listIterator() {
        return new ListItr(0);
    }

    /**
     * 以正确的顺序返回该列表中的元素的迭代器。
     * 返回的迭代器是fail-fast 。
     */
    public Iterator<E> iterator() {
        return new Itr();
    }
```

