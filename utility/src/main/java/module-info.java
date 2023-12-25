module utility {
    requires com.google.gson;
    requires java.sql;//for gson

    requires kafka.clients;
    requires org.apache.commons.codec;

    exports net.alexhyisen;
    exports net.alexhyisen.log;
}