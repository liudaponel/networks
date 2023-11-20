module nsu.snake {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires com.google.protobuf;

    opens nsu.snake to javafx.fxml;
    exports nsu.snake;
    exports nsu.snake.view;
    opens nsu.snake.view to javafx.fxml;
}