package io.github.spaceage_capitalist;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCombination;

public class TextEditorController {

    @FXML private AnchorPane root;
    @FXML private AnchorPane top_bar;
    @FXML private Button return_button;
    @FXML private Label file_name;
    @FXML private TextArea text_box;

    private Path filePath;
    private String spaceName;
    private boolean isSaved = true;

    @FXML
    private void initialize() {
        KeyCodeCombination saveShortcut =
                new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN);
        root.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (saveShortcut.match(event)) {
                saveFile();
                event.consume();
            } else if(text_box.isFocused()) {
                file_name.setText(filePath.getFileName().toString() + "*");
                isSaved = false;
            }
        });
    }

    void setFile(Path filePath, String spaceName) throws IOException {
        String contents = Files.readString(filePath, StandardCharsets.UTF_8);
        this.filePath = filePath;
        this.spaceName = spaceName;
        file_name.setText(filePath.getFileName().toString());
        text_box.setText(contents);
    }

    @FXML
    private void saveFile() {
        try {
            Files.writeString(filePath, text_box.getText(), StandardCharsets.UTF_8);
            file_name.setText(filePath.getFileName().toString());
            isSaved = true;
        } catch (IOException ex) {
            Dialogs.error("Could not save file: " + ex.getMessage());
        }
    }

    @FXML
    private void returnToSpace() throws IOException {
        if (!isSaved) {
            boolean quit = Dialogs.confirm("You have unsaved changes. Exit anyway?");
            if (!quit) {
                return;
            }
        }

        App.showSpace(spaceName);
    }
}