#  一、ConcurrentHashMap 1.7

## 1、存储结构

![](../assets/java7_concurrenthashmap.png)

Java 7 中 `ConcurrentHashMap` 的存储结构如上图，`ConcurrnetHashMap` 由很多个 `Segment` 组合，而每一个 `Segment` 是一个类似于 `HashMap` 的结构，所以每一个 `HashMap` 的内部可以进行扩容。但是 `Segment` 的个数一旦**初始化就不能改变**，默认 `Segment` 的个数是 16 个，你也可以认为 `ConcurrentHashMap` 默认支持最多 16 个线程并发。

## 2、初始化

通过 `ConcurrentHashMap` 的无参构造探寻 `ConcurrentHashMap` 的初始化流程。

```java
    /**
     * Creates a new, empty map with a default initial capacity (16),
     * load factor (0.75) and concurrencyLevel (16).
     */
    public ConcurrentHashMap() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL);
    }
```

无参构造中调用了有参构造，传入了三个参数的默认值，他们的值是：

```java
    /**
     * 默认初始化容量，即桶数组的默认大小。
     */
    static final int DEFAULT_INITIAL_CAPACITY = 16;

    /**
     * 默认负载因子
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * 默认并发级别，即分段锁（Segment）的数量
     * 决定了最多允许多少个线程 并发地修改不同段的数据而互不阻塞。
     */
    static final int DEFAULT_CONCURRENCY_LEVEL = 16;
```

接着看下这个有参构造函数的内部实现逻辑：

```java
@SuppressWarnings("unchecked")
public ConcurrentHashMap(int initialCapacity,float loadFactor, int concurrencyLevel) {
    // 参数校验
    if (!(loadFactor > 0) || initialCapacity < 0 || concurrencyLevel <= 0)
        throw new IllegalArgumentException();
    // 校验并发级别大小，大于 1<<16，重置为 65536
    if (concurrencyLevel > MAX_SEGMENTS)
        concurrencyLevel = MAX_SEGMENTS;
    // Find power-of-two sizes best matching arguments
    // 2的多少次方
    int sshift = 0;
    // ssize 是 Segment 数组的长度，也就是并发级别
    int ssize = 1;
    // 这个循环可以找到 concurrencyLevel 之上最接近的 2的次方值
    while (ssize < concurrencyLevel) {
        ++sshift;
        ssize <<= 1;
    }
    // 记录段偏移量
    this.segmentShift = 32 - sshift;
    // 记录段掩码
    this.segmentMask = ssize - 1;
    // 设置容量
    if (initialCapacity > MAXIMUM_CAPACITY)
        initialCapacity = MAXIMUM_CAPACITY;
    // c = 容量 / ssize ，默认 16 / 16 = 1，这里是计算每个 Segment 中的类似于 HashMap 的容量
    // 将 initialCapacity 平均分配到每个 Segment 上，每个 Segment 至少需要 c 个容量（或更多）来满足总容量要求。
    int c = initialCapacity / ssize;
    if (c * ssize < initialCapacity)
        ++c;
    int cap = MIN_SEGMENT_TABLE_CAPACITY;
    //Segment 中的类似于 HashMap 的容量至少是2或者2的倍数
    while (cap < c)
        cap <<= 1;
    // create segments and segments[0]
    // 创建 Segment 数组，设置 segments[0]
    // 只先初始化第一个 Segment（s0），其他在需要时惰性初始化。
    Segment<K,V> s0 = new Segment<K,V>(loadFactor, (int)(cap * loadFactor),
                         (HashEntry<K,V>[])new HashEntry[cap]);
    Segment<K,V>[] ss = (Segment<K,V>[])new Segment[ssize];
    UNSAFE.putOrderedObject(ss, SBASE, s0); // ordered write of segments[0]
    this.segments = ss;
}
```

总结一下在 Java 7 中 ConcurrentHashMap 的初始化逻辑。

- 必要参数校验；

- 校验并发级别 `concurrencyLevel` 大小，如果大于最大值，重置为最大值。无参构造**默认值是 16**；

- 寻找并发级别 `concurrencyLevel` 之上最近的 **2 的幂次方**值，作为初始化容量大小，**默认是 16**；

- 记录 `segmentShift` 偏移量，这个值为【容量 = 2 的 N 次方】中的 N，在后面 Put 时计算位置时会用到，**默认是 32 - sshift = 28**；

- 记录 `segmentMask`，默认是 ssize - 1 = 16 -1 = 15；

- **初始化 `segments[0]`**，**默认大小为 2**，**负载因子 0.75**，**扩容阀值是 2\*0.75=1.5**，插入第二个值时才会进行扩容；

## 3、put

接着上面的初始化参数继续查看 put 方法源码。

```java
/**
 * Maps the specified key to the specified value in this table.
 * Neither the key nor the value can be null.
 *
 * <p> The value can be retrieved by calling the <tt>get</tt> method
 * with a key that is equal to the original key.
 *
 * @param key key with which the specified value is to be associated
 * @param value value to be associated with the specified key
 * @return the previous value associated with <tt>key</tt>, or
 *         <tt>null</tt> if there was no mapping for <tt>key</tt>
 * @throws NullPointerException if the specified key or value is null
 */
public V put(K key, V value) {
    Segment<K,V> s;
    if (value == null)
        throw new NullPointerException();
    int hash = hash(key);
    // hash 值无符号右移 28位（初始化时获得），然后与 segmentMask=15 做与运算
    // 其实也就是把高4位与segmentMask（默认15，即1111）做与运算
    // 根据 key 的 hash 值计算其应该落在哪一个 Segment 上。
    int j = (hash >>> segmentShift) & segmentMask;
    if ((s = (Segment<K,V>)UNSAFE.getObject          // nonvolatile; recheck
         (segments, (j << SSHIFT) + SBASE)) == null) //  in ensureSegment
        // 如果查找到的 Segment 为空，线程安全地初始化该 Segment
        s = ensureSegment(j);
    // 调用该 Segment 的 put 方法，进行实际的哈希存储操作。
    // Segment 的 put 方法本质上就是一个 HashMap 的 put，但它是加了锁的（使用 ReentrantLock.lock()），所以线程安全。
    return s.put(key, hash, value, false);
}
```

> **获取 Segment 参数拆解：**
>
> * `segments`：Segment 数组
> * `j << SSHIFT`：计算当前 Segment 在数组中的内存偏移
> 	* `SSHIFT` 是每个 Segment 占用的字节大小的幂（通常是 3，因为 2³ = 8 bytes 对应一个引用）
> * `SBASE`：Segment 数组的起始地址偏移（base offset）
>
> ➡️ 整体含义就是：**取出第 j 个 Segment 对象（非 volatile 读）**
>
> ⚠️ 这里用 `UNSAFE.getObject` 而不是 `segments[j]`，原因是为了：
>
> * 更精细控制性能（绕过 volatile）
> * 避免内存屏障带来的性能开销



 **`ensureSegment(k)` 主要功能：**

* 确保 `segments[k]` 不为 null，如果是 null，则使用 `CAS + synchronized` 方式安全地初始化第 `k` 个 Segment；
* 并保证并发线程不会重复初始化同一个 Segment：使用 `UNSAFE.getObjectVolatile` 和 `CAS` 来实现线程安全的惰性初始化。

```java
/**
 * Returns the segment for the given index, creating it and
 * recording in segment table (via CAS) if not already present.
 *
 * @param k the index
 * @return the segment
 */
@SuppressWarnings("unchecked")
private Segment<K,V> ensureSegment(int k) {
    final Segment<K,V>[] ss = this.segments;
    long u = (k << SSHIFT) + SBASE; // raw offset
    Segment<K,V> seg;
    // 判断 u 位置的 Segment 是否为null
    if ((seg = (Segment<K,V>)UNSAFE.getObjectVolatile(ss, u)) == null) {
        // 用第一个已经初始化的 Segment（segments[0]）作为原型 proto，用它来拷贝一些通用参数。
        Segment<K,V> proto = ss[0]; // use segment 0 as prototype
        // 获取0号 segment 里的 HashEntry<K,V> 初始化长度
        int cap = proto.table.length;
        // 获取0号 segment 里的 hash 表里的扩容负载因子，所有的 segment 的 loadFactor 是相同的
        float lf = proto.loadFactor;
        // 计算扩容阀值
        int threshold = (int)(cap * lf);
        // 创建一个 cap 容量的 HashEntry 数组
        HashEntry<K,V>[] tab = (HashEntry<K,V>[])new HashEntry[cap];
        if ((seg = (Segment<K,V>)UNSAFE.getObjectVolatile(ss, u)) == null) { // recheck
            // 再次检查 u 位置的 Segment 是否为null，因为这时可能有其他线程进行了操作
            Segment<K,V> s = new Segment<K,V>(lf, threshold, tab);
            // 自旋检查 u 位置的 Segment 是否为null
            while ((seg = (Segment<K,V>)UNSAFE.getObjectVolatile(ss, u))
                   == null) {
                // 使用CAS 赋值，只会成功一次
                if (UNSAFE.compareAndSwapObject(ss, u, null, seg = s))
                    break;
            }
        }
    }
    return seg;
}
```



上面的源码分析了 `ConcurrentHashMap` 在 put 一个数据时的处理流程，下面梳理下具体流程。

1. 计算要 put 的 key 的位置，获取指定位置的 `Segment`。

2. 如果指定位置的 `Segment` 为空，则初始化这个 `Segment`。

  **初始化 Segment 流程：**

  1. 检查计算得到的位置的 `Segment` 是否为 null；
  2. 为 null 继续初始化，使用 `Segment[0]` 的容量和负载因子创建一个 `HashEntry` 数组；
  3. 再次检查计算得到的指定位置的 `Segment` 是否为 null；
  4. 使用创建的 `HashEntry` 数组初始化这个 Segment；
  5. 自旋（循环检查某个条件是否满足）判断计算得到的指定位置的 `Segment` 是否为 null，使用 CAS 在这个位置赋值为 `Segment`。

3. `Segment.put` 插入 key,value 值。



> **🧠 两次判断的作用？**
>
> ✅ 第一次判断（快速失败）：节省绝大多数线程开销
>
> ```java
> if ((seg = UNSAFE.getObjectVolatile(ss, u)) == null) {
> ```
>
> - 如果该 Segment 已经初始化好了（CAS 成功过了），
> 	- 后续其他线程可以**快速跳过整个初始化流程**
> 	- 避免：
> 		- 冗余对象构造
> 		- 冗余 CAS 操作（加锁）
>
> - 成功初始化的 Segment 会立即被 volatile 可见，其他线程立刻能看到
>
> ✅ 第二次判断（避免重复创建对象）：防止不必要的构造开销
>
> ```java
> if ((seg = UNSAFE.getObjectVolatile(ss, u)) == null) {
>     Segment<K,V> s = new Segment(...);
>     CAS(ss, u, null, s);
> }
> ```
>
> - **多个线程可能同时通过第一次判断，继续往下走**
>
> - 如果没有第二次判断，大家都会**白白构造 Segment 对象**（但只有一个能 CAS 成功）
>
> - 第二次判断在构造前再确认一次是否还需要构造，能大大减少不必要的内存/CPU 消耗
>
> 📊 举个极端例子对比
>
> | 场景                             | 没有第 2 次判断                               | 有第 2 次判断                                                |
> | -------------------------------- | --------------------------------------------- | ------------------------------------------------------------ |
> | 100 个线程并发初始化某个 segment | 100 个线程都构造一个 Segment，但只有 1 个生效 | 只有5个线程构造 Segment 对象，其余 95 个线程不会走到构造那步，但只有1个 Segment 对象 |
> | 资源消耗                         | 高：大量 Segment 对象白白构造、占内存         | 低：大部分线程只判断，什么都不做                             |
> | GC 压力                          | 大                                            | 小                                                           |
>
> ✅ 再强调一次两次判断的职责分工
>
> | 判断位置   | 作用                                     | 是否保证线程安全？    | 是否必要？            |
> | ---------- | ---------------------------------------- | --------------------- | --------------------- |
> | 第一次判断 | 提高性能，快速判断是否已初始化，避免浪费 | ❌（它不保证线程安全） | ✅（跳过绝大多数情况） |
> | 第二次判断 | 确保只构造必要的 Segment，避免重复构造   | ❌（仍靠 CAS 保证）    | ✅（性能优化）         |
> | CAS 操作   | 最终确保只有一个 Segment 被写入          | ✅（原子性保障）       | ✅（核心）             |
>
> 🧠 拓展一下：**那能不能判断 3 次、4 次甚至更多？**
>
> 理论上是：**可以的**。不过这有一个平衡问题：
>
> | 判断次数   | 好处（性能）                                  | 成本（判断逻辑） | 是否值得            |
> | ---------- | --------------------------------------------- | ---------------- | ------------------- |
> | 1 次       | 简单、逻辑直观，但资源浪费大（如 100 个对象） | ✅最轻            | ❌                   |
> | 2 次       | 大幅度减少构造的对象（如只构造 10 个）        | 稍复杂，但付出小 | ✅推荐方案           |
> | 3 次       | 可能进一步减少浪费（如只构造 3 个）           | 增加代码复杂度   | ⚠️可考虑，但收益递减 |
> | 4 次及以上 | 几乎不会再有明显收益了，且复杂度提升          | 成本 > 收益      | ❌不推荐             |
>
> 所以在 **并发编程中广泛使用的套路是“两次判断 + CAS”**，这也是 Java 源码中「懒加载 + 并发初始化」的经典写法，比如：
>
> * `ConcurrentHashMap` 初始化 `Segment`
> * `ThreadLocalMap` 初始化 `Entry`
> * `Double-Checked Locking（DCL）` 也用的是类似思路



上面探究了获取 `Segment` 段和初始化 `Segment` 段的操作。最后一行的 `Segment` 的 put 方法还没有查看，继续分析。

```java
final V put(K key, int hash, V value, boolean onlyIfAbsent) {
    // 获取 ReentrantLock 独占锁，获取不到，scanAndLockForPut 获取。
    HashEntry<K,V> node = tryLock() ? null : scanAndLockForPut(key, hash, value);
    V oldValue;
    try {
        HashEntry<K,V>[] tab = table;
        // 计算要put的数据位置
        int index = (tab.length - 1) & hash;
        // CAS 获取 index 坐标的值
        HashEntry<K,V> first = entryAt(tab, index);
        for (HashEntry<K,V> e = first;;) {
            if (e != null) {
                // 检查是否 key 已经存在，如果存在，则遍历链表寻找位置，找到后替换 value
                K k;
                if ((k = e.key) == key ||
                    (e.hash == hash && key.equals(k))) {
                    oldValue = e.value;
                    if (!onlyIfAbsent) {
                        e.value = value;
                        ++modCount;
                    }
                    break;
                }
                e = e.next;
            }
            else {
                // first 有值没说明 index 位置已经有值了，有冲突，链表头插法。
                if (node != null)
                    node.setNext(first);
                else
                    node = new HashEntry<K,V>(hash, key, value, first);
                int c = count + 1;
                // 容量大于扩容阀值，小于最大容量，进行扩容
                if (c > threshold && tab.length < MAXIMUM_CAPACITY)
                    rehash(node);
                else
                    // index 位置赋值 node，node 可能是一个元素，也可能是一个链表的表头
                    setEntryAt(tab, index, node);
                ++modCount;
                count = c;
                oldValue = null;
                break;
            }
        }
    } finally {
        unlock();
    }
    return oldValue;
}
```

> 📌 方法定义
>
> ```java
> final V put(K key, int hash, V value, boolean onlyIfAbsent)
> ```
>
> **参数说明：**
>
> * `key`: 要插入的键
> * `hash`: 预计算好的 key 的哈希值
> * `value`: 要插入的值
> * `onlyIfAbsent`: 如果为 `true`，当 key 存在时不会替换已有值；为 `false` 时会直接覆盖。
>
> 
>
> **🧩 步骤详解**
>
>  **1️⃣ 获取锁**
>
> ```java
> HashEntry<K,V> node = tryLock() ? null : scanAndLockForPut(key, hash, value);
> ```
>
> - `tryLock()`：尝试获取 `ReentrantLock` 独占锁，快速路径。
>
> - 获取失败，说明有竞争，进入 `scanAndLockForPut()`，进行一定次数的“自旋 + 检查 + 再尝试加锁”。
>
> - 如果 `tryLock()` 成功，`node` 为 null，说明竞争不激烈，因为还没有遍历哈希桶，所以 **不确定是否需要构造新节点**；
>
> 	> 🧠 总结：为什么这样设计？
> 	>
> 	> | 情况           | tryLock 成功（无竞争）     | tryLock 失败（有竞争）        |
> 	> | -------------- | -------------------------- | ----------------------------- |
> 	> | 是否获取锁     | ✅ 是，立即获取             | ❌ 否，需要自旋、扫描、等待    |
> 	> | 是否构造新节点 | ❌ 不确定是否需要，延迟构造 | ✅ 已知 key 不存在，提前构造   |
> 	> | node 返回值    | `null`（稍后可能再构造）   | 新构造的 `HashEntry`（备用）  |
> 	> | 优化目标       | 减少无用对象构造           | 缓解锁竞争、减少 CPU 资源浪费 |
>
> **2️⃣ 定位桶的位置**
>
> ```java
> HashEntry<K,V>[] tab = table;
> int index = (tab.length - 1) & hash;
> HashEntry<K,V> first = entryAt(tab, index);
> ```
>
> - `entryAt` 是一个 volatile 读取，保证多线程可见性。
>
> - `(tab.length - 1) & hash`：利用位运算定位到哈希桶数组中的某个 index，冲突时进入链表。
>
> 	> 假设哈希表 `tab` 的长度为 `2^n`（ConcurrentHashMap 中数组长度总是 2 的幂次方），那么 `(tab.length - 1)` 就是一个二进制全是 1 的数，等价于从 `hash` 值中**取低 n 位**作为数组下标；
> 	>
> 	> 使用 `&` 位运算比 `%` 求模快得多；
> 	>
> 	> Java 的 `%` 对负数不友好，`&` 运算没有这个问题；
>
> 3️⃣ 遍历链表查找
>
> ```java
> for (HashEntry<K,V> e = first;;) {
>     if (e != null) {
>         K k;
>         if ((k = e.key) == key || (e.hash == hash && key.equals(k))) {
>             oldValue = e.value;
>             if (!onlyIfAbsent) {
>                 e.value = value;
>                 ++modCount;
>             }
>             break;
>         }
>         e = e.next;
>     }
> ```
>
> - 遍历链表，判断 key 是否已存在。
>
> 	> **第一部分**：
> 	>
> 	> ```java
> 	> (k = e.key) == key
> 	> ```
> 	>
> 	> **如果是同一个对象（引用相等），直接判定相等，效率更高。**也避免了后续执行 `key.equals(k)` 时发生 `NullPointerException`。
> 	>
> 	> 
> 	>
> 	> **第二部分**：
> 	>
> 	> ```java
> 	> (e.hash == hash && key.equals(k))
> 	> ```
> 	>
> 	> - 如果不是同一个引用，那就继续判断：
> 	>
> 	> 	1. `hash` 是否相同（这是哈希表中的第一重判断，快速排除不匹配）；
> 	> 	2. 调用 `equals()` 方法做真正的值相等判断。
> 	>
> 	> 	
> 	>
> 	>  **✅ 为什么不能只写 `key.equals(k)`？**
> 	>
> 	> 因为：
> 	>
> 	> * 如果传入的 `key == null`，调用 `key.equals(...)` 就会抛出 **`NullPointerException`**。
> 	> * 即使传入的 `key` 不为 null，如果 `e.key` 为 null（即 `k` 是 null），也不应该去判断 `key.equals(k)`，因为这就不是有效的 key。
>
> - key 存在：
> 	- `onlyIfAbsent == false`：替换旧值。
> 	- 否则保留旧值。
>
> - `modCount++`：记录结构变更，用于 fail-fast 检测。
>
> 4️⃣ 插入新节点
>
> ```java
> else {
>     if (node != null)
>         node.setNext(first);
>     else
>         node = new HashEntry<K,V>(hash, key, value, first);
> ```
>
> - 如果链表为空（`e == null`），说明可以插入：
> 	- `node != null`：说明 `scanAndLockForPut` 中已构造好 `HashEntry`，只需挂接。
> 	- 否则创建新 `HashEntry` 对象，头插法挂在链表前面。
>
> 5️⃣ 检查是否需要扩容
>
> ```java
> int c = count + 1;
> if (c > threshold && tab.length < MAXIMUM_CAPACITY)
>     rehash(node);
> else
>     setEntryAt(tab, index, node);
> count = c;
> ```
>
> - 如果插入后数量超过阈值，则进行扩容并插入`node`（`rehash()`中执行），会重新分配更大的 table 并重排。
>
> - 否则直接通过 `setEntryAt` 将新节点放入指定位置。
>
> 6️⃣ 释放锁
>
> ```java
> } finally {
>     unlock();
> }
> ```
>
> - 确保无论中途发生什么异常，都会释放锁。
>
> 7️⃣ 返回旧值（如有）
>
> ```java
> return oldValue;
> ```
>
> - 如果 key 已存在并被替换，返回旧值。
>
> - 如果是新增 entry，返回 `null`。

由于 `Segment` 继承了 `ReentrantLock`，所以 `Segment` 内部可以很方便的获取锁，put 流程就用到了这个功能。

1. `tryLock()` 获取锁，获取不到使用 **`scanAndLockForPut`** 方法继续获取。

2. 计算 put 的数据要放入的 index 位置，然后获取这个位置上的 `HashEntry` 。

3. 遍历 put 新元素，为什么要遍历？因为这里获取的 `HashEntry` 可能是一个空元素，也可能是链表已存在，所以要区别对待。

  如果这个位置上的 **`HashEntry` 不存在**：

  1. 插入前先判断是否需要扩容（`c > threshold`），若需要扩容，**先执行 `rehash(node)`**；
  2. 否则，执行头插法插入。

  如果这个位置上的 **`HashEntry` 存在**：

  - “遍历链表”：
  	* 如果找到 key 相同的节点（通过 `==` 或 `equals` 判断）：
  		* 如果 `onlyIfAbsent == false`，则替换该节点的值；
  		* 返回旧值；
  	* 如果没找到：
  		* 判断是否需要扩容，若需要则执行 `rehash(node)`；
  		* 否则，将 `node` 头插到链表上；
  		* 更新计数，返回 null。

4. 如果要插入的位置之前已经存在，替换后返回旧值，否则返回 null.

这里面的第一步中的 `scanAndLockForPut` 操作这里没有介绍，这个方法做的操作就是不断的自旋 `tryLock()` 获取锁。当自旋次数大于指定次数时，使用 `lock()` 阻塞获取锁。在自旋时顺表获取下 hash 位置的 `HashEntry`。

```java
private HashEntry<K,V> scanAndLockForPut(K key, int hash, V value) {
    HashEntry<K,V> first = entryForHash(this, hash);
    HashEntry<K,V> e = first;
    HashEntry<K,V> node = null;
    int retries = -1; // negative while locating node
    // 自旋获取锁
    while (!tryLock()) {
        HashEntry<K,V> f; // to recheck first below
        if (retries < 0) {
            if (e == null) {
                if (node == null) // speculatively create node
                    node = new HashEntry<K,V>(hash, key, value, null);
                retries = 0;
            }
            else if (key.equals(e.key))
                retries = 0;
            else
                e = e.next;
        }
        else if (++retries > MAX_SCAN_RETRIES) {
            // 自旋达到指定次数后，阻塞等到只到获取到锁
            lock();
            break;
        }
        else if ((retries & 1) == 0 &&
                 (f = entryForHash(this, hash)) != first) {
            e = first = f; // re-traverse if entry changed
            retries = -1;
        }
    }
    return node;
}
```

## 4、扩容 rehash

`ConcurrentHashMap` 的扩容只会扩容到原来的两倍。老数组里的数据移动到新的数组时，位置要么不变，要么变为 `index+ oldSize`，参数里的 node 会在扩容之后使用链表**头插法**插入到指定位置。

```java
private void rehash(HashEntry<K,V> node) {
    HashEntry<K,V>[] oldTable = table;
    // 老容量
    int oldCapacity = oldTable.length;
    // 新容量，扩大两倍
    int newCapacity = oldCapacity << 1;
    // 新的扩容阀值
    threshold = (int)(newCapacity * loadFactor);
    // 创建新的数组
    HashEntry<K,V>[] newTable = (HashEntry<K,V>[]) new HashEntry[newCapacity];
    // 新的掩码，默认2扩容后是4，-1是3，二进制就是11。
    int sizeMask = newCapacity - 1;
    for (int i = 0; i < oldCapacity ; i++) {
        // 遍历老数组
        HashEntry<K,V> e = oldTable[i];
        if (e != null) {
            HashEntry<K,V> next = e.next;
            // 计算新的位置，新的位置只可能是不变或者是老的位置+老的容量。
            int idx = e.hash & sizeMask;
            if (next == null)   //  Single node on list
                // 如果当前位置还不是链表，只是一个元素，直接赋值
                newTable[idx] = e;
            else { // Reuse consecutive sequence at same slot
                // 如果是链表了
                HashEntry<K,V> lastRun = e;
                int lastIdx = idx;
                // 新的位置只可能是不变或者是老的位置+老的容量。
                // 遍历结束后，lastRun 后面的元素位置都是相同的
                for (HashEntry<K,V> last = next; last != null; last = last.next) {
                    int k = last.hash & sizeMask;
                    if (k != lastIdx) {
                        lastIdx = k;
                        lastRun = last;
                    }
                }
                // ，lastRun 后面的元素位置都是相同的，直接作为链表赋值到新位置。
                newTable[lastIdx] = lastRun;
                // Clone remaining nodes
                for (HashEntry<K,V> p = e; p != lastRun; p = p.next) {
                    // 遍历剩余元素，头插法到指定 k 位置。
                    V v = p.value;
                    int h = p.hash;
                    int k = h & sizeMask;
                    HashEntry<K,V> n = newTable[k];
                    newTable[k] = new HashEntry<K,V>(h, p.key, v, n);
                }
            }
        }
    }
    // 头插法插入新的节点
    int nodeIndex = node.hash & sizeMask; // add the new node
    node.setNext(newTable[nodeIndex]);
    newTable[nodeIndex] = node;
    table = newTable;
}
```

- 这里第一个 for 是为了寻找这样一个节点，这个节点后面的所有 next 节点的新位置都是相同的。然后把这个作为一个链表赋值到新位置。

- 第二个 for 循环是为了把剩余的元素通过头插法插入到指定位置链表。

内部第二个 `for` 循环中使用了 `new HashEntry<K,V>(h, p.key, v, n)` 创建了一个新的 `HashEntry`，而不是复用之前的，是因为如果复用之前的，那么会导致正在遍历（如正在执行 `get` 方法）的线程由于指针的修改无法遍历下去。正如注释中所说的：

> 当它们不再被可能正在并发遍历表的任何读取线程引用时，被替换的节点将被垃圾回收。
>
> The nodes they replace will be garbage collectable as soon as they are no longer referenced by any reader thread that may be in the midst of concurrently traversing table

为什么需要再使用一个 `for` 循环找到 `lastRun` ，其实是为了减少对象创建的次数，正如注解中所说的：

> 从统计上看，在默认的阈值下，当表容量加倍时，只有大约六分之一的节点需要被克隆。
>
> Statistically, at the default threshold, only about one-sixth of them need cloning when a table doubles.

## 5、get

到这里就很简单了，get 方法只需要两步即可。

1. 计算得到 key 的存放位置。
2. 遍历指定位置查找相同 key 的 value 值。

```java
public V get(Object key) {
    Segment<K,V> s; // manually integrate access methods to reduce overhead
    HashEntry<K,V>[] tab;
    int h = hash(key);
    long u = (((h >>> segmentShift) & segmentMask) << SSHIFT) + SBASE;
    // 计算得到 key 的存放位置
    if ((s = (Segment<K,V>)UNSAFE.getObjectVolatile(segments, u)) != null &&
        (tab = s.table) != null) {
        for (HashEntry<K,V> e = (HashEntry<K,V>) UNSAFE.getObjectVolatile
                 (tab, ((long)(((tab.length - 1) & h)) << TSHIFT) + TBASE);
             e != null; e = e.next) {
            // 如果是链表，遍历查找到相同 key 的 value。
            K k;
            if ((k = e.key) == key || (e.hash == h && key.equals(k)))
                return e.value;
        }
    }
    return null;
}
```

# 二、ConcurrentHashMap 1.8
## 1、存储结构

![](../assets/java8_concurrenthashmap.png)

可以发现 Java8 的 ConcurrentHashMap 相对于 Java7 来说变化比较大，不再是之前的 **Segment 数组 + HashEntry 数组 + 链表**，而是 **Node 数组 + 链表 / 红黑树**。当冲突链表达到一定长度时，链表会转换成红黑树。

## 2、初始化 initTable

```java
/**
 * Initializes table, using the size recorded in sizeCtl.
 */
private final Node<K,V>[] initTable() {
    Node<K,V>[] tab; 
    int sc;
    while ((tab = table) == null || tab.length == 0) {
        //　如果 sizeCtl < 0 ,说明另外的线程执行CAS 成功，正在进行初始化。
        if ((sc = sizeCtl) < 0)
            // 让出 CPU 使用权
            Thread.yield(); // lost initialization race; just spin
        else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
            try {
                if ((tab = table) == null || tab.length == 0) {
                    int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                    @SuppressWarnings("unchecked")
                    Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                    table = tab = nt;
                    sc = n - (n >>> 2);
                }
            } finally {
                sizeCtl = sc;
            }
            break;
        }
    }
    return tab;
}
```

从源码中可以发现 `ConcurrentHashMap` 的初始化是通过**自旋和 CAS** 操作完成的。里面需要注意的是成员变量 `sizeCtl` （sizeControl 的缩写），它的值决定着当前的初始化状态。

1. `-1` 说明正在初始化，其他线程需要自旋等待
2. `-N`  说明 table 正在进行扩容，高 16 位表示扩容的标识戳，低 16 位减 1 为正在进行扩容的线程数
3. `0`  表示 table 初始化大小，如果 table 没有初始化
4. `>0 ` 表示 table 扩容的阈值，如果 table 已经初始化。
