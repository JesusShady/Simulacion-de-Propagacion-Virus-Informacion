module com.example.simulacionpropagacion {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires gs.core;
    requires gs.ui.javafx;

    opens com.example.simulacionpropagacion to javafx.fxml;
    exports com.example.simulacionpropagacion;
}