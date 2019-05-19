package net.alexhyisen.log;

public enum LogCls {
    DEFAULT("cout", "log"),
    MAIL("mail"),
    BOOK("book"),
    SALE("sale"),
    LOOP("loop"),
    INFO("info");

    private final String desc;
    private final String path;

    LogCls(String desc, String path) {
        this.desc = desc;
        this.path = path;
    }

    LogCls(String desc) {
        this(desc, desc + "_log");
    }

    public String getDesc() {
        return desc;
    }

    public String getPath() {
        return path;
    }
}
