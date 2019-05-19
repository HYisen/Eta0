package net.alexhyisen.log;

import net.alexhyisen.Utility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.EnumMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class LocalLog implements Log {
    //achieve stateless contract through static member
    private static Function<LogCls, Path> pathFinder = Utility.genCachedMapper(new EnumMap<>(LogCls.class),
            v -> Paths.get(".", v.getPath()));
    private static Function<LogCls, ReentrantLock> lockFinder = Utility.genCachedMapper(new EnumMap<>(LogCls.class),
            v -> new ReentrantLock());

    @Override
    public void log(LogCls type, String message) {
        final var path = pathFinder.apply(type);
        final var lock = lockFinder.apply(type);
        final var msg = map(type, message) + "\n";
        System.out.print(msg);
        try {
            lock.lock();
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
            Files.write(
                    path,
                    msg.getBytes(),
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
}

