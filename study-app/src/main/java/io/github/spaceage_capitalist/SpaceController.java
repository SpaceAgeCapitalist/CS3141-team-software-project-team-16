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

public class SpaceController {

    @FXML private BorderPane root;
    @FXML private Label titleLabel;
    @FXML private FlowPane fileContainer;

    private String spaceName;

    @FXML
    private void initialize() {
        // Right-click on empty space -> "Add File" / "New Quiz"
        ContextMenu menu = new ContextMenu();

        MenuItem addFile = new MenuItem("Add File");
        addFile.setOnAction(e -> addFile());

        MenuItem newQuiz = new MenuItem("New Quiz");
        newQuiz.setOnAction(e -> newQuiz());

        menu.getItems().addAll(addFile, newQuiz);

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

    /** Asks for a quiz name, then opens the quiz editor page. */
    private void newQuiz() {
        Dialogs.askName("New Quiz", "Name your quiz", "").ifPresent(name -> {
            if (!SpaceStorage.isValidName(name)) {
                Dialogs.error("Please enter a name that doesn't use these characters: \\ / : * ? \" < > |");
                return;
            }
            if (SpaceStorage.quizExists(spaceName, name)) {
                Dialogs.error("A quiz with that name already exists in this study space.");
                return;
            }
            try {
                App.showQuizEditor(spaceName, name.trim());
            } catch (IOException ex) {
                Dialogs.error("Could not open the quiz editor: " + ex.getMessage());
            }
        });
    }
        private void editQuiz(Path file) {
        try {
            Quiz quiz = SpaceStorage.loadQuiz(file);
            App.showQuizEditor(spaceName, quiz);
        } catch (IOException ex) {
            Dialogs.error("Could not open quiz for editing: " + ex.getMessage());
        }
    }

    private void openFile(Path file) {
        // Quizzes are not opened by another program. Taking a quiz is a later story,
        // so for now show a short summary to confirm what was saved.
        if (SpaceStorage.isQuizFile(file)) {
            try {
                Quiz quiz = SpaceStorage.loadQuiz(file);
                int n = quiz.getQuestions().size();
                Dialogs.info("Quiz: " + quiz.getName() + "\n" + n + (n == 1 ? " question" : " questions")
                        + "\n\nTaking quizzes is coming in a later update.");
            } catch (IOException ex) {
                Dialogs.error("Could not read quiz: " + ex.getMessage());
            }
            return;
        }

        if (!Desktop.isDesktopSupported()) {
            Dialogs.error("Opening files isn't supported on this system.");
            return;
        }
        new Thread(() -> {
            try {
                Desktop.getDesktop().open(file.toFile());
            } catch (Exception ex) {
                Platform.runLater(() -> Dialogs.error("Could not open file: " + ex.getMessage()));
            }
        }).start();
    }

    private void deleteFile(Path file) {
        String label = SpaceStorage.isQuizFile(file)
                ? "quiz \"" + SpaceStorage.quizName(file) + "\""
                : "\"" + file.getFileName() + "\"";
        if (!Dialogs.confirm("Delete " + label + "?")) return;
        try {
            SpaceStorage.deleteFile(file);
            refresh();
        } catch (IOException ex) {
            Dialogs.error("Could not delete file: " + ex.getMessage());
        }
    }

    private void refresh() {
        fileContainer.getChildren().clear();
        try {
            for (Path file : SpaceStorage.listFiles(spaceName)) {
                String text = SpaceStorage.isQuizFile(file)
                        ? "Quiz: " + SpaceStorage.quizName(file)
                        : file.getFileName().toString();
                Button b = new Button(text);
                b.setPrefSize(140, 90);
                b.setWrapText(true);
                b.setOnAction(e -> openFile(file));

                ContextMenu menu = new ContextMenu();
                if (SpaceStorage.isQuizFile(file)) {
                    MenuItem edit = new MenuItem("Edit");
                    edit.setOnAction(e -> editQuiz(file));
                    menu.getItems().add(edit);
                }
                MenuItem delete = new MenuItem("Delete");
                delete.setOnAction(e -> deleteFile(file));
                menu.getItems().add(delete);
                b.setOnContextMenuRequested(e -> {
                    menu.show(b, e.getScreenX(), e.getScreenY());
                    e.consume();
                });

                fileContainer.getChildren().add(b);
            }
        } catch (IOException ex) {
            Dialogs.error("Could not load files: " + ex.getMessage());
        }
    }
}