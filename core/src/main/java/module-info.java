module core {
    requires utility;
    requires book;
    requires mail;
    requires sale;

    requires com.google.gson;
    requires java.sql;//for gson
    requires io.netty.all;
    requires website;

    opens net.alexhyisen.eta.core to com.google.gson;
    exports net.alexhyisen.eta.core;
}