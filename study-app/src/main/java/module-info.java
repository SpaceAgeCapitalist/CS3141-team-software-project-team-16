module io.github.spaceage_capitalist {
    requires transitive javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    opens io.github.spaceage_capitalist to javafx.fxml;
    exports io.github.spaceage_capitalist;
}
