package net.alexhyisen.eta.model;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.PrettyXmlSerializer;
import org.htmlcleaner.TagNode;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Created by Alex on 2017/2/14.
 * Some valuable functions.
 */
public class Utility {
    @Nullable
    public static byte[] download(String url) {
        //Utility.stamp("download 0");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream is = null;
        try {
            URLConnection connection = new URL(url).openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:52.0) Gecko/20100101 Firefox/52.0");
            is = connection.getInputStream();
            byte[] byteChunk = new byte[4096]; // Or whatever size you want to read in at a time.
            int n;

            while ((n = is.read(byteChunk)) > 0) {
                baos.write(byteChunk, 0, n);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        //Utility.stamp("download 1");
        byte[] data = baos.toByteArray();
        if (data.length < 1000) {
            Utility.log(LogCls.BOOK, "data is suspiciously small from " + url);
        }
        return data;
    }

    @SuppressWarnings("unused")
    @Nullable
    public static byte[] clean(String url) {
        return clean(download(url));
    }

    @Nullable
    public static byte[] clean(@Nullable byte[] source) {
        //Utility.stamp("clean start");
        if (source == null) {
            return null;
        }

        CleanerProperties props = new CleanerProperties();

        props.setTranslateSpecialEntities(true);
        props.setTransResCharsToNCR(true);
        props.setOmitComments(true);

        HtmlCleaner cleaner = new HtmlCleaner();
        TagNode tg;
        try {
            //cleaner.clean(new URL(url)) failed to get the correct charset.
            //page "http://www.fhxiaoshuo.com/read/67/67220/" as an example.
            //Utility.stamp("clean 0");
            tg = cleaner.clean(new ByteArrayInputStream(source));
            //Utility.stamp("clean 1");

            //Utility.stamp("check 0");
            final String KEY = "charset=";
            TagNode node = tg
                    .findElementByAttValue("http-equiv", "Content-Type", true, false);
            String charset;
            if (node != null) {
                String content = node.getAttributeByName("content");
                charset = content.substring(content.indexOf(KEY) + KEY.length());
            } else {
                charset = "UTF-8";
            }

            //System.out.println("charset = "+charset+" | "+props.getCharset());
            if (!"utf-8".equalsIgnoreCase(charset)) {
                tg = cleaner.clean(new ByteArrayInputStream(source), charset);
            }
            //Utility.stamp("check 1");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        //Utility.stamp("output 0");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            new PrettyXmlSerializer(props).writeToStream(tg, os, "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        //Utility.stamp("output 1");
        return os.toByteArray();
    }

    //Note that such a wrapper is redundant if a resource pool is used.
    //collections.defaultdict or @lru_cache in Python, laziness is a virtue.
    //Thread Safe. How simple it would be if only I have a Global Interpreter Lock.
    //While a global static SingleThreadExecutor is also a potential reasonable solution.
    @SuppressWarnings("WeakerAccess")
    public static <K, V> Function<K, V> genCachedMapper(Map<K, V> inner, Function<K, V> mapper) {
        return new Function<>() {
            private ReentrantLock lock = new ReentrantLock();

            @Override
            public V apply(K k) {
                V value = inner.get(k);
                if (value == null) {
                    lock.lock();
                    //Despite it's idempotent, sleeping is better than useless working.
                    //Therefore a second try in critical section is used to prevent it.
                    try {
                        value = inner.get(k);
                        if (value == null) {
                            value = mapper.apply(k);
                            inner.put(k, value);
                        }
                    } finally {
                        //Is a LockGuard implements Closeable that enable try with resources better?
                        lock.unlock();
                    }
                }
                return value;
            }
        };
    }

    private static Function<LogCls, Path> pathFinder = genCachedMapper(new EnumMap<>(LogCls.class),
            v -> Paths.get(".", v.getFilename()));
    private static Function<LogCls, ReentrantLock> lockFinder = genCachedMapper(new EnumMap<>(LogCls.class),
            v -> new ReentrantLock());

    public static void log(String msg) {
        log(LogCls.DEFAULT, msg);
    }

    public static void log(LogCls cls, String msg) {
        final var path = pathFinder.apply(cls);
        final var lock = lockFinder.apply(cls);
        final var message = String.format("[%s] %s %s\n", cls.getDesc(), LocalDateTime.now().toString(), msg);
        System.out.print(message);
        try {
            lock.lock();
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
            Files.write(
                    path,
                    message.getBytes(),
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public enum LogCls {
        DEFAULT("cout", "log"),
        MAIL("mail"),
        BOOK("book"),
        SALE("sale"),
        LOOP("loop"),
        INFO("info");

        private final String desc;
        private final String filename;

        LogCls(String desc, String filename) {
            this.desc = desc;
            this.filename = filename;
        }

        LogCls(String desc) {
            this(desc, "log_" + desc);
        }

        public String getDesc() {
            return desc;
        }

        public String getFilename() {
            return filename;
        }
    }

    public static IntStream revRange(int from, int to) {
        return IntStream.range(to, from).map(i -> from - 1 - (i - to));
    }

    private static Instant record = null;

    @SuppressWarnings("WeakerAccess")
    public static void stamp(String msg) {
        if (record == null) {
            record = Instant.now();
        }
        long interval = Duration.between(record, Instant.now()).toMillis();
        record = Instant.now();
        System.out.printf("T+%4dms %s\n", interval, msg);
    }
}
