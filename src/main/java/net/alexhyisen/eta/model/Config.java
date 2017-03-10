package net.alexhyisen.eta.model;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alex on 2017/3/6.
 * Config is a configs' serialization solution.
 */

public class Config {
    private Map<String,String> data=new HashMap<>();
    private final Path path;

    public Config() {
        this.path=Paths.get(".","config");
        if(Files.exists(path)){
            load();
        }
    }

    //That explicit constructor is only used for redirect path in Test s
    public Config(Path path) {
        this.path = path;
        if(Files.exists(path)){
            load();
        }
    }

    public String get(String key){
        return data.get(key);
    }

    public String put(String key,String value){
        return data.put(key, value);
    }

    public boolean save(){
        try {
            PrintWriter pw=new PrintWriter(new FileWriter((path.toFile()),false));
            data.entrySet().stream()
                    .map(v->v.getKey()+"="+v.getValue())
                    .forEach(pw::println);
            pw.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean load(){
        data=new HashMap<>();
        try {
            Files.lines(path)
                    .map(str->str.split("="))
                    .forEach(array->data.put(array[0],array[1]));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
