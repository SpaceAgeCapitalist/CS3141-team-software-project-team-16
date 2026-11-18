package io.github.spaceage_capitalist;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.event.Event;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;

public class TextEditor implements TabController {
    private TabPane tabPane;
    private Tab spaceTab;
    private Tab editorTab;

    private Path filePath;
    private Consumer<TabController> onClosedCallback;
    private TextEditorController controller;

    private class TextEditorController {
        @FXML private AnchorPane root;
        @FXML private AnchorPane top_bar;
        @FXML private TextArea text_box;
        
        private boolean isSaved = true;
        
        @FXML
        private void initialize() {
            KeyCodeCombination saveShortcut = new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN);
            root.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (saveShortcut.match(event)) {
                    saveFile();
                    event.consume();
                }
            });

            String content;
            try {
                content = Files.readString(filePath, StandardCharsets.UTF_8);
            } catch (IOException e) {
                System.out.println("Warning: file " + filePath.getFileName() + " failed to open properly.");
                content = "";
            }

            text_box.setText(content);

            text_box.textProperty().addListener((observable, oldText, newText) -> {
                editorTab.setText(filePath.getFileName().toString() + "*");
                isSaved = false;
            });
        }

        @FXML
        private boolean saveFile() {
            if (isSaved) {
                return true;
            }

            try {
                Files.writeString(filePath, text_box.getText(), StandardCharsets.UTF_8);
                editorTab.setText(filePath.getFileName().toString());
                isSaved = true;
                return true;
            } catch (IOException ex) {
                Dialogs.error("Could not save file: " + ex.getMessage());
                return false;
            }
        }
    }

    public TextEditor(Path file, SpaceController spaceController, Consumer<TabController> onClosedCallback, boolean startFocused) throws IOException {
        // Initialize the text editor with the given file
        this.filePath = file;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("TextEditor.fxml"));
        TextEditorController controller = new TextEditorController();

        this.controller = controller;
        this.tabPane = spaceController.getTabPane();
        this.editorTab = new Tab(file.getFileName().toString());
        editorTab.setOnCloseRequest(this::handleCloseRequest);

        loader.setController(controller);
        Parent editorRoot = loader.load();
        
        editorTab.setContent(editorRoot);
        this.tabPane.getTabs().add(editorTab);

        this.onClosedCallback = onClosedCallback;

        if (startFocused) {
            this.tabPane.getSelectionModel().select(editorTab);
        }
    }

    public TextEditor(Path file, SpaceController spaceController,Consumer<TabController> onClosedCallback) throws IOException {
        this(file, spaceController, onClosedCallback, true);
    }

    private void handleCloseRequest(Event event) {
        closeTabRequest();
        event.consume();
    }

    public String getTitle() {
        return editorTab.getText();
    }

    /** Focuses the tab */
    public void focusTab() {
        tabPane.getSelectionModel().select(editorTab);
    }

    /** Force closes the tab */
    public void closeTab() {
        tabPane.getTabs().remove(editorTab);
        tabPane.getSelectionModel().select(spaceTab);
        onClosedCallback.accept(this);
    }

    /** Requests for the tab to close, allowing the tab to decide whether to close, prompt the user, or ignore the request.
     *  @return True if the tab was closed, false otherwise. 
    */
    public boolean closeTabRequest() {
        boolean consent = getCloseConsent();
        if(consent) {
            closeTab();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean getCloseConsent() {
        if(!controller.isSaved) {
            focusTab();

            int response = Dialogs.unsavedChanges("Do you want to save the changes you made to " + filePath.getFileName() + "?");

            if(response == 0) {
                // User selected "cancel"
                return false;
            } else if(response == 1) {
                // Exit without saving
                return true;
            } else {
                // Save changes and exit
                if(controller.saveFile()) {
                    return true;
                } else {
                    // Dont close the file if an error occures and it cant be saved.
                    return false;
                }
            }
        }

        return true;
    }
}
