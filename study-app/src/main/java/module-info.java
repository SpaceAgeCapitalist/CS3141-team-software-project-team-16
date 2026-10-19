module io.github.spaceage_capitalist {
    requires javafx.controls;
    requires javafx.fxml;

    opens io.github.spaceage_capitalist to javafx.fxml;
    exports io.github.spaceage_capitalist;
}
