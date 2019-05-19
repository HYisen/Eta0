module core {
    requires utility;
    requires book;
    requires mail;
    requires sale;

    requires gson;
    requires java.sql;//for gson
    requires netty.all;

    exports net.alexhyisen.eta.core;
}