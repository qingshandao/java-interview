package com.javaiodemo.InputStream;

import java.io.*;

public class DataInputStreamDemo {
    public static void main(String[] args) {
        // 1.按顺序写入不同数据类型数据
        DataOutputStreamDemo();

        // 2.按顺序读取不同数据类型数据
        DataInputStreamDemo();

    }

    /**
     * 使用 DataOutputStream 写入指定类型的数据
     */
    public static void DataOutputStreamDemo(){
        try(FileOutputStream fileOutputStream = new FileOutputStream("./output.txt")){
            // 0.基于 FileOutputStream 创建 DataOutputStream
            DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);

            // 1.写入boolean
            dataOutputStream.writeBoolean(true);

            // 2.写入int
            dataOutputStream.writeInt(100);

            // 3.写入UTF字符串
            dataOutputStream.writeUTF("hello world");

            // 4.关闭 DataOutputStream
            dataOutputStream.close();

        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public static void DataInputStreamDemo(){
        try(InputStream fileInputStream = new FileInputStream("./output.txt")){
            // 0.基于 FileInputStream 创建 DataInputStream
            DataInputStream dataInputStream = new DataInputStream(fileInputStream);

            // 1.读取boolean
            boolean readBoolean = dataInputStream.readBoolean();
            System.out.println("读取到的 boolean 类型数据为：" + readBoolean);

            // 2.读取int
            int readInt = dataInputStream.readInt();
            System.out.println("读取到的 int 类型数据为：" + readInt);

            // 3.读取UTF字符串
            String readUTF = dataInputStream.readUTF();
            System.out.println("读取到的 String 类型数据为：" + readUTF);

            // 4.关闭DataInputStream
            dataInputStream.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
