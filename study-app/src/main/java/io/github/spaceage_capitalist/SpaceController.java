package io.github.spaceage_capitalist;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SpaceController {

    @FXML private BorderPane root;
    @FXML private Label titleLabel;
    @FXML private FlowPane fileContainer;

    private String spaceName;

    @FXML
    private void initialize() {
        // Right-click on empty space -> "Add File"
        ContextMenu menu = new ContextMenu();
        MenuItem addFile = new MenuItem("Add File");
        addFile.setOnAction(e -> addFile());
        menu.getItems().add(addFile);

        // Right-click on empty space -> "New Flashcard Set"
        MenuItem newFlashcards = new MenuItem("New Flashcard Set");
        newFlashcards.setOnAction(e -> createFlashcardSet());
        menu.getItems().add(newFlashcards);

        root.setOnContextMenuRequested(e -> {
            menu.show(root, e.getScreenX(), e.getScreenY());
            e.consume();
        });
    }

    /** Called by App right after this page loads. */
    void setSpace(String name) {
        this.spaceName = name;
        titleLabel.setText(name);
        refresh();
    }

    @FXML
    private void goHome() throws IOException {
        App.setRoot("primary", "Study App");
    }

    private void addFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose a file to add");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Study files", "*.mp4", "*.pdf", "*.jpg", "*.jpeg", "*.png"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        File chosen = chooser.showOpenDialog(root.getScene().getWindow());
        if (chosen == null) return;

        // Suggest the original name (minus extension) as a starting point
        String original = chosen.getName();
        int dot = original.lastIndexOf('.');
        String suggestion = dot > 0 ? original.substring(0, dot) : original;

        Dialogs.askName("Name your file", "What should this file be called?", suggestion).ifPresent(name -> {
            if (!SpaceStorage.isValidName(name)) {
                Dialogs.error("Please enter a name that doesn't use these characters: \\ / : * ? \" < > |");
                return;
            }
            try {
                if (!SpaceStorage.addFile(spaceName, chosen, name)) {
                    Dialogs.error("A file with that name already exists in this study space.");
                    return;
                }
                refresh();
            } catch (IOException ex) {
                Dialogs.error("Could not add file: " + ex.getMessage());
            }
        });
    }

    private void openFile(Path file) {
        if (!Desktop.isDesktopSupported()) {
            Dialogs.error("Opening files isn't supported on this system.");
            return;
        }
        // Run off the JavaFX thread so the app doesn't freeze while the other program launches
        new Thread(() -> {
            try {
                Desktop.getDesktop().open(file.toFile());
            } catch (Exception ex) {
                Platform.runLater(() -> Dialogs.error("Could not open file: " + ex.getMessage()));
            }
        }).start();
    }

    private void deleteFile(Path file) {
        if (!Dialogs.confirm("Delete \"" + file.getFileName() + "\"?")) return;
        try {
            SpaceStorage.deleteFile(file);
            refresh();
        } catch (IOException ex) {
            Dialogs.error("Could not delete file: " + ex.getMessage());
        }
    }

    // right-click to delte a flash card set
    private void deleteFlashcardSet(String name) {
        if (!Dialogs.confirm("Delete \"" + name + "\"?")){ 
            return;
        }
        try {
            SpaceStorage.deleteFlashcardSet(spaceName, name);
            refresh();
        } catch (IOException ex) {
            Dialogs.error(
                "Could not delete flashcard set: " + ex.getMessage()
            );
        }
    }

    private void refresh() {
        fileContainer.getChildren().clear();
        try {
            for (Path file : SpaceStorage.listFiles(spaceName)) {
                Button b = new Button(file.getFileName().toString());
                b.setPrefSize(140, 90);
                b.setWrapText(true);
                b.setOnAction(e -> openFile(file));

                ContextMenu menu = new ContextMenu();
                MenuItem delete = new MenuItem("Delete");
                delete.setOnAction(e -> deleteFile(file));
                menu.getItems().add(delete);
                b.setOnContextMenuRequested(e -> {
                    menu.show(b, e.getScreenX(), e.getScreenY());
                    e.consume();
                });

                fileContainer.getChildren().add(b);
            }

            for (Flashcards set : SpaceStorage.listFlashcardSets(spaceName)) {
                Button button = new Button("Flashcards: " + set.getName());
                button.setPrefSize(140, 90);
                button.setWrapText(true);

                button.setOnAction(e -> {
                    try {
                        openFlashcardEditor(set, true);
                    } catch (IOException ex) {
                        Dialogs.error(
                            "Could not open flashcards: " + ex.getMessage()
                        );
                    }
                });

                ContextMenu menu = new ContextMenu();
                MenuItem delete = new MenuItem("Delete");
                delete.setOnAction(e -> deleteFlashcardSet(set.getName()));
                menu.getItems().add(delete);

                button.setOnContextMenuRequested(e -> {
                    menu.show(button, e.getScreenX(), e.getScreenY());
                    e.consume();
                });

                fileContainer.getChildren().add(button);
            }
        } 
        catch (IOException ex) {
            Dialogs.error("Could not load files: " + ex.getMessage());
        }
    }

    // after right clicking it calls this method to create a new empty flashcard set
    private void createFlashcardSet() {
        Dialogs.askName(
            "New Flashcard Set",
            "Name your flashcard set",
            ""
        ).ifPresent(name -> {
            if (!SpaceStorage.isValidName(name)) {
                Dialogs.error("Please enter a valid flashcard set name.");
                return;
            }

            String setName = name.trim();

            try {
                if (SpaceStorage.hasFlashcardSet(spaceName, setName)) {
                    Dialogs.error(
                        "A flashcard set with that name already exists "
                        + "in this study space."
                    );
                    return;
                }

                Flashcards set = new Flashcards(setName);
                openFlashcardEditor(set, false);

            } catch (IOException ex) {
                Dialogs.error(
                    "Could not create flashcard set: " + ex.getMessage()
                );
            }
        });
    }

    // after clicking on the flashcard set it will start this method which 
    // sets up the editor to make and edit the flashcards
    private void openFlashcardEditor( Flashcards set, 
        boolean editingExisting) throws IOException {

        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("flashcard.fxml")
        );

        Parent editorRoot = loader.load();

        FlashcardController controller = loader.getController();
        controller.setSpaceName(spaceName);
        controller.setEditingExisting(editingExisting);
        controller.setFlashcardSet(set);

        Stage editorWindow = new Stage();
        editorWindow.initOwner(root.getScene().getWindow());
        editorWindow.setTitle(spaceName + " - " + set.getName());
        editorWindow.setScene(new Scene(editorRoot));

        editorWindow.setOnHidden(e -> refresh());

        editorWindow.show();
    }
}