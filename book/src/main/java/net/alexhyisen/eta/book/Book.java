package net.alexhyisen.eta.book;

import net.alexhyisen.Utility;

import javax.xml.bind.annotation.XmlElement;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by Alex on 2017/2/22.
 * Book is the avatar of a book, including a Index and many Chapter s.
 */
public class Book {
    private Index index;
    private List<Chapter> chapters;

    private String source;
    private String path;

    private String name;

    private boolean cached = false;

    public Book(String source, String path, String name) {
        this.source = source;
        this.path = path;
        this.name = name;
    }

    public Book() {
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Book && this.source.equals(((Book) obj).source) && this.path.equals(((Book) obj).path) && this.name.equals(((Book) obj).name);
    }

    @Override
    public int hashCode() {
        return source.hashCode();
    }

    @XmlElement(name = "link")
    public String getSource() {
        return source;
    }

    @XmlElement(name = "path")
    public String getPath() {
        return path;
    }

    @XmlElement(name = "name")
    public String getName() {
        return name;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Chapter> getChapters() {
        return chapters;
    }

    public boolean isCached() {
        return cached;
    }

    public boolean isOpened() {
        return chapters != null;
    }

    //What's the difference between open() and read()?
    //You open a book first and then get its index, later can you read the chapters.
    //To make it easier, open() init index & chapters, while read() preload chapters' data
    //from the Internet, which is the most time consuming procedure.

    public void open() {
        Utility.log("opening " + name);
        index = new Index(source, path);
        chapters = index.getData().entrySet().stream()
                .map(v -> new Chapter(v.getValue().getHref(), v.getKey(), v.getValue().getText(), path))
                .collect(Collectors.toList());
    }

    private static String genChapterFrame(Chapter chapter) {
        return "\n\n\n" + chapter.getName() + "\n\n" +
                Arrays
                        .stream(chapter.getData())
                        .reduce("", (a, b) -> a + "\n" + b);
    }

    private void read(ExecutorService exec, Consumer<Chapter> downloadFunc) {
        if (index == null) {
            open();
        }
        chapters.forEach(downloadFunc);
        exec.shutdown();
        cached = true;
    }

    public void read(int nThreads) {
        ExecutorService exec = Executors.newFixedThreadPool(nThreads);
        read(exec, v -> v.download(exec));
    }

    public List<Chapter> save() {
        return getChapters().stream().filter(Chapter::writeIfUncached).collect(Collectors.toList());
    }

    public void read(int nThreads, LongAdder adder) {
        // I would use a channel in golang to transmit the message.
        // But BlockingQueue in Java might not be sufficient and therefore elegant enough.

        ExecutorService exec = Executors.newFixedThreadPool(nThreads);
        read(exec, v -> v.download(exec, adder));
    }

    public void archive() {
        try (Writer writer = new FileWriter(this.getName() + ".txt")) {
            writer.write("《" + this.getName() + "》\n\n\n\n");
            getChapters()
                    .stream()
                    .map(Book::genChapterFrame)
                    .forEach(v -> {
                        try {
                            writer.write(v);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String build() {
        return getChapters().stream().map(Book::genChapterFrame).collect(Collectors.joining());
    }
}
