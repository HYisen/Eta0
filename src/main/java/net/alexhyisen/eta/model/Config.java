package net.alexhyisen.eta.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Alex on 2017/3/6.
 * Config is a configs' serialization solution.
 */

public class Config {
    private Map<String,String> data=new LinkedHashMap<>();
    private final Path path;

    public Config() {
        this.path=Paths.get(".","config");
    }

    //That explicit constructor is only used for redirect path in Test s
    public Config(Path path) {
        this.path = path;
    }

    public String get(String key){
        return data.get(key);
    }

    public String put(String key,String value){
        return data.put(key, value);
    }

    public boolean save(){
        try {
            Gson gson=new GsonBuilder().setPrettyPrinting().create();
            Files.deleteIfExists(path);
            Files.write(path,gson.toJson(data).getBytes(),StandardOpenOption.CREATE_NEW);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean load(){
        data=new LinkedHashMap<>();//I want to keep the config content in insert order
        try {
            Gson gson=new GsonBuilder().setPrettyPrinting().create();
            data=gson.fromJson(new String(Files.readAllBytes(path)),new TypeToken<Map<String,String>>(){}.getType());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
