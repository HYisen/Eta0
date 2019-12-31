package net.alexhyisen.eta.website;

import net.alexhyisen.Utility;
import net.alexhyisen.log.LogCls;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Overseer {
    private final Path path;

    public Overseer(Path path) throws IOException {
        if (!path.toFile().isDirectory()) {
            throw new IOException("expect path " + path.toAbsolutePath() + " to be a directory");
        }
        this.path = path;
    }

    public static void main(String[] args) throws IOException {
        Overseer overseer = new Overseer(Path.of("."));
        overseer.maintain();
    }

    public void maintain() throws IOException {
        maintain(path);
    }

    private void maintain(Path rootPath) throws IOException {
        File[] files = rootPath.toFile().listFiles();
        if (files == null) {
            throw new IOException(new NullPointerException("result of listFiles() shall not be null"));
        }

        Set<String> total = new HashSet<>();
        Set<String> exist = new HashSet<>();
        List<File> subDirs = new ArrayList<>();
        for (File childFile : files) {
            if (childFile.isDirectory() && !childFile.getName().endsWith(".assets") && childFile.getName().startsWith(".")) {
                subDirs.add(childFile);
            } else {
                String filename = childFile.getName();
                String name = Utility.extractName(childFile.toPath());
                if (filename.endsWith(".md")) {
                    total.add(name);
                } else if (filename.endsWith(".html")) {
                    exist.add(name);
                }
            }
        }
        total.removeAll(exist);

        for (String name : total) {
            Path target = Path.of(rootPath.toString(), name + ".md");
            Utility.log(LogCls.LOOP, "generate " + target.toAbsolutePath());
            Transformer.generate(target);
        }
        for (File subDir : subDirs) {
            maintain(subDir.toPath());
        }
    }
}
