module gui {
    requires utility;
    requires book;
    requires mail;

    requires javafx.base;
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;

    opens net.alexhyisen.eta.gui to javafx.fxml, javafx.graphics;
    exports net.alexhyisen.eta.gui;

}