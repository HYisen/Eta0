module book {
    requires utility;

    requires java.xml.bind;
    requires com.sun.xml.bind;//for jaxb
    requires jsr305;
    requires htmlcleaner;

    exports net.alexhyisen.eta.book;
    opens net.alexhyisen.eta.book to java.xml.bind, javafx.base;
}