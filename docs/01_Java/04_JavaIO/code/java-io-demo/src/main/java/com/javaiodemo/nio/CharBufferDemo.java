package com.javaiodemo.nio;

import java.nio.CharBuffer;

public class CharBufferDemo {
    public static void main(String[] args) {
        // 分配一个容量为8的 CharBuffer
        CharBuffer buffer = CharBuffer.allocate(8);
        System.out.println("初始状态：");
        printState(buffer);

        // 向 Buffer 中写入3哥字符：
        buffer.put('a').put('b').put('c');
        System.out.println("写入3个字符后的状态：");
        printState(buffer);

        // 调用 flip 切换为读模式，limit=position，position=0，mark=-1
        buffer.flip();
        System.out.println("调用flip()后的状态：");
        printState(buffer);

        // 读取字符
        System.out.println("buffer中的内容：");
        while (buffer.hasRemaining()){
            System.out.print(buffer.get());
        }
        System.out.println("\n");

        // 调用 clear()，缓冲区的内容被彻底清空，position = 0，limit = capacity
        buffer.clear();
        System.out.println("调用clear()方法后的状态：");
        printState(buffer);
    }

    /**
     * 输出当前 CharBuffer 的状态
     * @param buffer
     */
    private static void printState(CharBuffer buffer){
        System.out.print("capacity: " + buffer.capacity());
        System.out.print(", limit: " + buffer.limit());
        System.out.print(", position: " + buffer.position());
        System.out.println(", mark 开始读取的字符：" + buffer.mark() + "\n");
    }
}
