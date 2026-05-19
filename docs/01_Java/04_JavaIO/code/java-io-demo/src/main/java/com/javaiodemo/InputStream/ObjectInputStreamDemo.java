package com.javaiodemo.InputStream;

import com.javaiodemo.InputStream.entity.Person;

import java.io.*;

public class ObjectInputStreamDemo {
    public static void main(String[] args) {

        // 1.序列化对象
        ObjectOutputFunc();

        // 2.反序列化
        ObjectInputFunc();
    }

    /**
     * 对象序列化，写入对象
     */
    public static void ObjectOutputFunc(){
        // 1.创建对象
        Person person = new Person("张三", "作家", "123456");

        // 2.创建对象输出流
        try(FileOutputStream fileOutputStream = new FileOutputStream("./person.txt")){

            // 1.创建对象输出流
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);

            // 2.将对象写入指定文件序列化
            objectOutputStream.writeObject(person);

            // 3.关闭对象输出流
            objectOutputStream.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * 对象反序列化读取
     */
    public static void ObjectInputFunc(){
        // 1.创建文件输入流
        try(FileInputStream fileInputStream = new FileInputStream("./person.txt")){
            // 2.创建对象输入流，反序列化
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);

            // 3.读取对象
            Person person = (Person)objectInputStream.readObject();
            System.out.println("读取到的对象为：" + person);

            // 4.关闭对象输入流
            objectInputStream.close();
        }catch (IOException e){
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
