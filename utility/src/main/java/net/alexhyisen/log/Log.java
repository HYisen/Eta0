package net.alexhyisen.log;

import java.time.LocalDateTime;

public interface Log {
    void log(LogCls type, String message);

    /**
     * As the name suggest, because its optional implantation is expected to locate in a Singleton,
     * the shutdown procedure is global, which means should only be executed once in the final procedure.
     */
    default void shutdownGlobally() {
    }

    default String map(LogCls type, String message) {
        return String.format("[%s] %s %s", type.getDesc(), LocalDateTime.now().toString(), message);
    }
}
