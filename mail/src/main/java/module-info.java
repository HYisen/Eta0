module mail {
    requires utility;
    requires book;

    requires jsr305;
    requires netty.all;

    exports net.alexhyisen.eta.mail;
}