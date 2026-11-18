package io.github.spaceage_capitalist;

import java.io.IOException;

import com.jfoenix.controls.JFXHamburger;
import com.jfoenix.transitions.hamburger.HamburgerSlideCloseTransition;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;

public class PrimaryController {

    @FXML private BorderPane root;
    @FXML private FlowPane spaceContainer;

    private ContextMenu openMenu;
    @FXML private JFXHamburger haml;
    @FXML private VBox sidePanel;

    @FXML
    private void initialize() {
        // Right-click on empty space -> "New Study Space"
        ContextMenu menu = new ContextMenu();
        MenuItem newSpace = new MenuItem("New Study Space");
        newSpace.setOnAction(e -> createSpace());
        menu.getItems().add(newSpace);

        root.setOnContextMenuRequested(e -> {
            showMenu(menu, root, e.getScreenX(), e.getScreenY());
            e.consume();
        });
        root.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> hideMenu());

        //hameburger
        HamburgerSlideCloseTransition transition = new HamburgerSlideCloseTransition(haml);
        transition.setRate(-1);
        haml.addEventHandler(MouseEvent.MOUSE_CLICKED, (e) -> {
            boolean opening = !sidePanel.isVisible();

            sidePanel.setVisible(opening);
            sidePanel.setManaged(opening);

            transition.setRate(opening ? 1 : -1);
            transition.play();
        });

        refresh();
    }

    //helper methodds for above code blocks
    private void showMenu(ContextMenu menu, Node owner, double x, double y) {
        hideMenu();
        menu.show(owner, x, y);
        openMenu = menu;
    }

    private void hideMenu() {
        if (openMenu != null) {
            openMenu.hide();
            openMenu = null;
        }
    }

    private void renameSpace(String oldName) {
        Dialogs.askName("Rename Study Space", "Enter a new name for \"" + oldName + "\"", oldName)
                .ifPresent(name -> {
            if (!SpaceStorage.isValidName(name)) {
                Dialogs.error("Please enter a name that doesn't use these characters: \\ / : * ? \" < > |");
                return;
            }
            try {
                if (!SpaceStorage.renameSpace(oldName, name)) {
                    Dialogs.error("A study space with that name already exists.");
                    return;
                }
                refresh();
            } catch (IOException ex) {
                Dialogs.error("Could not rename study space: " + ex.getMessage());
            }
        });
    }

    
    @FXML
    private void closeApp() {
        Platform.exit();
    }

    @FXML
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

                // Right-click a folder -> Rename / Delete (*adjusted*)
                ContextMenu menu = new ContextMenu();
                MenuItem rename = new MenuItem("Rename");
                rename.setOnAction(e -> renameSpace(name));
                MenuItem delete = new MenuItem("Delete");
                delete.setOnAction(e -> deleteSpace(name));
                menu.getItems().addAll(rename, delete);
                b.setOnContextMenuRequested(e -> {
                    showMenu(menu, b, e.getScreenX(), e.getScreenY());
                    e.consume();
                });

                spaceContainer.getChildren().add(b);
            }
        } catch (IOException ex) {
            Dialogs.error("Could not load study spaces: " + ex.getMessage());
        }
    }
}