module net.alexhyisen.eta.legacy {
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;

    requires gson;
    requires java.sql;//for gson

    requires htmlcleaner;

    requires netty.all;

    requires org.jsoup;

    requires jsr305;
    requires java.xml.bind;
    requires com.sun.xml.bind;//for jaxb

    opens net.alexhyisen.eta.view to javafx.fxml,javafx.graphics;
    opens net.alexhyisen.eta.model.catcher to java.xml.bind,javafx.base;
}