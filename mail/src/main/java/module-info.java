module mail {
    requires utility;
    requires book;

    requires jsr305;
    requires io.netty.all;

    exports net.alexhyisen.eta.mail;
}