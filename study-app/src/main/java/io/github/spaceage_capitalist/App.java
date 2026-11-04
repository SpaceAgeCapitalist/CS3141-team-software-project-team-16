package io.github.spaceage_capitalist;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private static Scene scene;
    private static Stage stage;

    @Override
    public void start(Stage primaryStage) throws IOException {
        stage = primaryStage;
        scene = new Scene(loadFXML("primary"), 640, 480);
        stage.setTitle("Study App");
        stage.setScene(scene);
        stage.show();
    }

    static void setRoot(String fxml, String title) throws IOException {
        scene.setRoot(loadFXML(fxml));
        stage.setTitle(title);
    }

    static void showSpace(String name) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("space.fxml"));
        Parent root = loader.load();
        SpaceController controller = loader.getController();
        controller.setSpace(name);
        scene.setRoot(root);
        stage.setTitle(name);
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }

}