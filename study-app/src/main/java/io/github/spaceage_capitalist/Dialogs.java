package io.github.spaceage_capitalist;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;

import java.util.Optional;

public class Dialogs {

    public static void error(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }

    public static void info(String message) {
        new Alert(Alert.AlertType.INFORMATION, message).showAndWait();
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

    public static int unsavedChanges(String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText(message);
        alert.setContentText("Your changes will be lost if you don't save them.");

        ButtonType optionSave = new ButtonType("Save");
        ButtonType optionDontSave = new ButtonType("Don't Save");
        ButtonType optionCancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(optionSave, optionDontSave, optionCancel);

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent()) {
            if (result.get() == optionSave) {
                return 2;
            } else if (result.get() == optionDontSave) {
                return 1;
            } else {
                // Handles optionCancel or clicking the window's 'X' close button
                return 0;
            }
        } else {
            return 0;
        }
    }
}