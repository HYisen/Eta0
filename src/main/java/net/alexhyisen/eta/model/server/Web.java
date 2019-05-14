package net.alexhyisen.eta.model.server;

import net.alexhyisen.eta.model.Utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @since 1.3
 * a holder of static web reference
 */
public class Web {
    private static final String IGNORE_SUFFIX = ".html";
    private static ConcurrentHashMap<String, File> data = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        var web = new Web();
        web.load();
    }

    private void load(File[] files, String prefix) {
        for (File file : files) {
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                if (children != null) {
                    prefix += file.getName() + "/";
                    load(children, prefix);
                }
            } else if (file.isFile()) {
                String name = file.getName();
                if (name.endsWith(IGNORE_SUFFIX)) {
                    name = name.substring(0, name.lastIndexOf(IGNORE_SUFFIX));
                }
                name = prefix + name;
                data.put(name, file);
            }
        }
    }

    public void load() throws IOException {
        var path = Paths.get(".", "www");
        if (!path.toFile().exists()) {
            Utility.log(Utility.LogCls.LOOP, "create web dir at " + path.toAbsolutePath().toString());
            Files.createDirectories(path);
        }

        final File[] files = path.toFile().listFiles();
        if (files != null) {
            data.clear();
            load(files, "/");
        }

        data.forEach((k, v) -> Utility.log(Utility.LogCls.LOOP, String.format(
                "load : %s -> %s", k, v.toPath().toAbsolutePath())));
    }

    public Optional<RandomAccessFile> get(String name) {
        return Optional.ofNullable(data.get(name)).map(v -> {
            try {
                return new RandomAccessFile(v, "r");
            } catch (FileNotFoundException e) {
                Utility.log(Utility.LogCls.LOOP, "failed to find " + v);
                e.printStackTrace();
                data.remove(name);
                return null;
            }
        });
    }
}
