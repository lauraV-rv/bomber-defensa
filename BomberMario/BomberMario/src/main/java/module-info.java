module com.example.bombermario {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens com.example.bombermario to javafx.fxml;
    exports com.example.bombermario;
    exports com.example.bombermario.control;
    opens com.example.bombermario.control to javafx.fxml;
    exports com.example.bombermario.ui;
    opens com.example.bombermario.ui to javafx.fxml;
}