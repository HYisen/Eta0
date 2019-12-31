module core {
    requires utility;
    requires book;
    requires mail;
    requires sale;

    requires gson;
    requires java.sql;//for gson
    requires io.netty.all;
    requires website;

    opens net.alexhyisen.eta.core to gson;
    exports net.alexhyisen.eta.core;
}