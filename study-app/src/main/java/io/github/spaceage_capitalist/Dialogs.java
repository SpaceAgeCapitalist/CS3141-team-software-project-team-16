package io.github.spaceage_capitalist;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;

import java.util.Optional;

public class Dialogs {

    public static void error(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }

    public static boolean confirm(String message) {
        Optional<ButtonType> r = new Alert(Alert.AlertType.CONFIRMATION, message).showAndWait();
        return r.isPresent() && r.get() == ButtonType.OK;
    }

    public static Optional<String> askName(String title, String header, String defaultText) {
        TextInputDialog d = new TextInputDialog(defaultText);
        d.setTitle(title);
        d.setHeaderText(header);
        d.setContentText("Name:");
        return d.showAndWait();
    }
}