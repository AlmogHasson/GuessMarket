package fx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.Objects;

public class GuessMarketApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(GuessMarketApp.class.getResource("main.fxml"));

        Font hiro = Font.loadFont(
           GuessMarketApp.class.getResourceAsStream("fonts/aAsianHiro.ttf"), 12);

        Scene scene = new Scene(loader.load(), 1200, 700);

        scene.getStylesheets().add(
                Objects.requireNonNull(GuessMarketApp.class
                                .getResource("themes/dark.css"))
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
