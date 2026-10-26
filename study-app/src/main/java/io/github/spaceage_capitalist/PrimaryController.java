package io.github.spaceage_capitalist;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

public class PrimaryController {

    @FXML private BorderPane root;
    @FXML private FlowPane spaceContainer;

    @FXML
    private void initialize() {
        // Right-click on empty space -> "New Study Space"
        ContextMenu menu = new ContextMenu();
        MenuItem newSpace = new MenuItem("New Study Space");
        newSpace.setOnAction(e -> createSpace());
        menu.getItems().add(newSpace);

        root.setOnContextMenuRequested(e -> {
            menu.show(root, e.getScreenX(), e.getScreenY());
            e.consume();
        });

        refresh();
    }

    private void createSpace() {
        Dialogs.askName("New Study Space", "Name your study space", "").ifPresent(name -> {
            if (!SpaceStorage.isValidName(name)) {
                Dialogs.error("Please enter a name that doesn't use these characters: \\ / : * ? \" < > |");
                return;
            }
            try {
                if (!SpaceStorage.createSpace(name)) {
                    Dialogs.error("A study space with that name already exists.");
                    return;
                }
                refresh();
            } catch (IOException ex) {
                Dialogs.error("Could not create study space: " + ex.getMessage());
            }
        });
    }

    private void deleteSpace(String name) {
        if (!Dialogs.confirm("Delete \"" + name + "\" and all its files?")) return;
        try {
            SpaceStorage.deleteSpace(name);
            refresh();
        } catch (IOException ex) {
            Dialogs.error("Could not delete study space: " + ex.getMessage());
        }
    }

    private void refresh() {
        spaceContainer.getChildren().clear();
        try {
            for (String name : SpaceStorage.listSpaces()) {
                Button b = new Button(name);
                b.setPrefSize(140, 90);
                b.setWrapText(true);

                b.setOnAction(e -> {
                    try {
                        App.showSpace(name);
                    } catch (IOException ex) {
                        Dialogs.error("Could not open study space: " + ex.getMessage());
                    }
                });

                // Right-click a folder -> Delete
                ContextMenu menu = new ContextMenu();
                MenuItem delete = new MenuItem("Delete");
                delete.setOnAction(e -> deleteSpace(name));
                menu.getItems().add(delete);
                b.setOnContextMenuRequested(e -> {
                    menu.show(b, e.getScreenX(), e.getScreenY());
                    e.consume();
                });

                spaceContainer.getChildren().add(b);
            }
        } catch (IOException ex) {
            Dialogs.error("Could not load study spaces: " + ex.getMessage());
        }
    }
}