package net.alexhyisen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Alex on 2017/3/6.
 * Config is a configs' serialization solution.
 */

public class Config {
    private final Path path;
    private Map<String, String> data = new LinkedHashMap<>();

    private static Config singleton = null;

    /**
     * Don't expose it outside unless the multi-thread write issue is solved.
     * Use it carefully.
     * @return an instance of the default Config which is not thread safe
     */
    private static Config getInstance() {
        if (singleton == null) {
            //My first time to use synchronized() in real project.
            //Whoever care the implementation of a one-run method? (spin lock, heavy lock, AQS, native code)
            synchronized (Config.class) {
                if (singleton == null) {
                    singleton = new Config();
                    singleton.load();
                }
            }
        }
        return singleton;
    }

    public static String getFromDefault(String key) {
        //don't use singleton instead than getter for consistence
        return getInstance().get(key);
    }

    public Config() {
        this.path = Paths.get(".", "config");
    }

    //That explicit constructor is only used for redirect path in Test s
    public Config(Path path) {
        this.path = path;
    }

    public Map<String, String> getData() {
        return data;
    }

    public String get(String key) {
        return data.get(key);
    }

    @SuppressWarnings("UnusedReturnValue")
    public String put(String key, String value) {
        return data.put(key, value);
    }

    public boolean save(Path path) {
        //data.forEach((k,v)-> System.out.println(k+"="+v));
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.deleteIfExists(path);
            Files.write(path, gson.toJson(data).getBytes(), StandardOpenOption.CREATE_NEW);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean save() {
        return save(path);
    }

    public boolean load(Path path) {
        data = new LinkedHashMap<>();//I want to keep the config content in insert order
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            data = gson.fromJson(new String(Files.readAllBytes(path)), new TypeToken<Map<String, String>>() {
            }.getType());
            //data.forEach((k,v)-> System.out.println(k+"="+v));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean load() {
        return load(path);
    }
}
