package net.alexhyisen;

import net.alexhyisen.log.KafkaLog;
import net.alexhyisen.log.LocalLog;
import net.alexhyisen.log.Log;
import net.alexhyisen.log.LogCls;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
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
            v -> Paths.get(".", v.getPath()));
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

    private static Log log;

    static {
        var config = new Config();
        config.load();
        System.out.println(config.get("logClazz"));
        System.out.println(config.get("kafkaBootstrapServers"));
        if ("KafkaLog".equals(config.get("logClazz")) &&
                config.get("kafkaBootstrapServers") != null) {
            System.out.println("use KafkaLog");
            log = KafkaLog.getInstance();
        } else {
            System.out.println("use LocalLog");
            log = new LocalLog();
        }
    }

//      "logClazz":"KafkaLog",
//  "kafkaBootstrapServers":"localhost:9092"

    public static void shutdownGlobally() {
        log.shutdownGlobally();
    }

    public static void log(LogCls cls, String msg) {
        log.log(cls, msg);
    }

    public static IntStream revRange(int from, int to) {
        return IntStream.range(to, from).map(i -> from - 1 - (i - to));
    }

    public static void stamp(String msg) {
        if (record == null) {
            record = Instant.now();
        }
        long interval = Duration.between(record, Instant.now()).toMillis();
        record = Instant.now();
        System.out.printf("T+%4dms %s\n", interval, msg);
    }
}
