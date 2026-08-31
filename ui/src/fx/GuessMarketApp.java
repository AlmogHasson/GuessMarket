package fx;

import api.GMController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class GuessMarketApp extends Application {



    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(GuessMarketApp.class.getResource("main.fxml"));

        Scene scene = new Scene(loader.load(), 1200, 700);

        scene.getStylesheets().add(
                GuessMarketApp.class
                        .getResource("darkMode.css")
                        .toExternalForm()
        );

        stage.setTitle("Guess Market");
        stage.setMinWidth(800);
        stage.setMinHeight(550);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
