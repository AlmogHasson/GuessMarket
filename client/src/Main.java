import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import util.HttpClientUtil;
import util.ThemeManager;

import java.util.Objects;

/**
 * Entry point for the ex3 client.
 *
 * The application opens on the login page. LoginController replaces the
 * scene's root once the server accepts the name, so this class never builds
 * the main screen and the stage is created exactly once.
 */
public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        // Registered for the whole JVM run; the Japanese theme refers to it by
        // family name. A null return means the file is not on the classpath -
        // the app still runs, it just falls back to the default font.
        Font hiro = Font.loadFont(getClass().getResourceAsStream("/fonts/aAsianHiro.ttf"), 12);
        if (hiro == null) {
            System.err.println("Font not loaded: /fonts/aAsianHiro.ttf");
        }

        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource("/fxmls/login.fxml"),
                        "/fxmls/login.fxml not found on the classpath"));

        Parent loginRoot = loader.load();

        Scene scene = new Scene(loginRoot, 520, 560);
        ThemeManager.apply(scene);           // the default theme, on the scene

        stage.setTitle("Guess Market - Login");
        stage.setMinWidth(460);
        stage.setMinHeight(520);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();

        // Opens the connection and loads OkHttp's classes while the user types a name.
        HttpClientUtil.warmUp();

        // Short pause so the login page is painted before the warm-up occupies the FX thread.
        PauseTransition delay = new PauseTransition(Duration.millis(300));
        delay.setOnFinished(e -> warmUp());
        delay.play();
    }

    /**
     * Builds and styles a throwaway main screen that is never shown.
     *
     * The first real login was slow and every later one fast, which is the
     * signature of one-time JVM/JavaFX work: loading the control and skin
     * classes, parsing the theme CSS, the first layout of the big tree. Doing
     * that here, while the user is still typing a name, means the first real
     * login finds it all done. The throwaway root is not reused on purpose -
     * it carries its own MainController state, and a failed login or a logout
     * would then have to reason about whether it is still fresh.
     *
     * Best effort: a failure here must never get in the way of logging in.
     */
    private static void warmUp() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    Main.class.getResource("/fxmls/main.fxml")));
            Scene offscreen = new Scene(root, 1200, 700);
            ThemeManager.apply(offscreen);
            root.applyCss();
            root.layout();
        } catch (Exception e) {
            System.err.println("Warm-up skipped: " + e);
        }
    }

    /**
     * Frees the user name on the server when the window closes, so the same
     * name can be used again without restarting Tomcat. Best effort only - if
     * the server is already gone there is nothing to report to anyone.
     */
    @Override
    public void stop() {
        try {
            HttpClientUtil.logout();
        } catch (Exception ignored) {
            // the window is closing; nowhere useful to surface this
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
