package net.alexhyisen.eta.model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Created by Alex on 2017/2/13.
 */
public class Test {
    public static void main(String[] args) {
        try {
            Writer w=new FileWriter(new File("D:\\Code\\output\\"+"第八百二十一章 欢迎回来"));
            w.write("aurora");
            w.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
