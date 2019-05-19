package net.alexhyisen;

import java.io.IOException;
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
    private static Function<LogCls, Path> pathFinder = genCachedMapper(new EnumMap<>(LogCls.class),
            v -> Paths.get(".", v.getFilename()));
    private static Function<LogCls, ReentrantLock> lockFinder = genCachedMapper(new EnumMap<>(LogCls.class),
            v -> new ReentrantLock());
    private static Instant record = null;

    public static String genDesc(int number, String singularNoun, String pluralNoun) {
        return number + " " + (number > 1 ? pluralNoun : singularNoun);
    }

    public static String genDesc(int number, String noun) {
        return genDesc(number, noun, noun + "s");
    }

    //Note that such a wrapper is redundant if a resource pool is used.
    //collections.defaultdict or @lru_cache in Python, laziness is a virtue.
    //Thread Safe. How simple it would be if only I have a Global Interpreter Lock.
    //While a global static SingleThreadExecutor is also a potential reasonable solution.
    //
    //Well, must adopt that ConcurrentHashMap::computeIfAbsent is the simplest and therefore the best answer,
    //I just skipped it when looking for a solution at that time.
    //Under benchmark of 10M query of 20 random key, there is only <1% difference in average cost time.
    //I keep my redundant implement of the identical interface just in memory of the youth.
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
                            //WARNING
                            //If Map::put is not atomic enough,
                            //parallel get on put may be corrupted.
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

    public static IntStream revRange(int from, int to) {
        return IntStream.range(to, from).map(i -> from - 1 - (i - to));
    }

    @SuppressWarnings("WeakerAccess")
    public static void stamp(String msg) {
        if (record == null) {
            record = Instant.now();
        }
        long interval = Duration.between(record, Instant.now()).toMillis();
        record = Instant.now();
        System.out.printf("T+%4dms %s\n", interval, msg);
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
            this(desc, desc + "_log");
        }

        public String getDesc() {
            return desc;
        }

        public String getFilename() {
            return filename;
        }
    }
}
