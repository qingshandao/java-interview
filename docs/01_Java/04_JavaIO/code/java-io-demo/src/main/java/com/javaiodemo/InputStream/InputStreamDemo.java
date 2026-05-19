package com.javaiodemo.InputStream;

import java.io.FileInputStream;
import java.io.IOException;

public class InputStreamDemo {
    public static void main(String[] args) {
        try(FileInputStream fileInputStream = new FileInputStream("./input.txt")){
            System.out.println("文件可读取的字节数为：" + fileInputStream.available());

            long skip = fileInputStream.skip(2);
            System.out.println("跳过的字节数为：" + skip);

            System.out.println("开始读取内容：");
            int content;
            while ((content = fileInputStream.read()) != -1){
                System.out.print((char)content);
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
