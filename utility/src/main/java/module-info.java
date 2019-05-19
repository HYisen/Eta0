module utility {
    requires gson;
    requires java.sql;//for gson

    requires kafka.clients;

    exports net.alexhyisen;
    exports net.alexhyisen.log;
}