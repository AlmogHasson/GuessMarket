package controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import util.Constants;
import util.FrameProbe;
import util.HttpClientUtil;
import util.ThemeManager;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField userNameField;

    @FXML
    private Button loginBtn;

    @FXML
    private Label errorLabel;

    @FXML
    private ComboBox<String> themeComboBox;

    @FXML
    private Label serverLabel;

    private long clickNanos;                                                      // TEMP timing

    @FXML
    private void initialize() {
        themeComboBox.getItems().setAll(ThemeManager.THEMES);
        themeComboBox.setValue(ThemeManager.getCurrent());
        serverLabel.setText("Server: " + Constants.BASE_URL);
        showError("");
        setLoading(false);
    }

    /** Also fires once from setValue() above, while the scene is still null - apply() ignores that. */
    @FXML
    private void setTheme() {
        ThemeManager.apply(themeComboBox.getScene(), themeComboBox.getValue());
    }

    @FXML
    private void login() {

        String userName = userNameField.getText().trim();

        if (userName.isEmpty()) {
            showError("Please enter a user name.");
            return;
        }

        clickNanos = System.nanoTime();                                           // TEMP timing
        setLoading(true);
        showError("");

        Task<Void> loginTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                long t0 = System.nanoTime();                                          // TEMP timing
                HttpClientUtil.login(userName);
                System.out.println("[timing] login request: " + (System.nanoTime() - t0) / 1_000_000 + " ms");
                return null;
            }
        };

        // Stay in the loading state until the main screen has replaced this one.
        loginTask.setOnSucceeded(event -> Platform.runLater(() -> openMainScreen(userName)));

        loginTask.setOnFailed(event -> {
            setLoading(false);
            Throwable error = loginTask.getException();

            if (error instanceof HttpClientUtil.ServerException serverException) {
                if (serverException.getStatusCode() == 409) {
                    showError("This user is already logged in.");
                } else {
                    String message = serverException.getMessage();
                    showError(message == null || message.isBlank() ? "Login failed." : message);
                }
            } else {
                showError(error == null || error.getMessage() == null ? "Could not connect to the server." : error.getMessage());
            }
        });

        Thread thread = new Thread(loginTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void openMainScreen(String userName) {

        try {
            long t0 = System.nanoTime();                                              // TEMP timing
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmls/main.fxml"));
            Parent root = loader.load();
            System.out.println("[timing] main.fxml load: " + (System.nanoTime() - t0) / 1_000_000 + " ms");
            MainController controller = loader.getController();

            controller.setUserName(userName);

            Stage stage = (Stage) userNameField.getScene().getWindow();

            // Resize first, while the light login page is still the root, and swap
            // the root last: the big main tree is then styled and laid out once, at
            // its final size, instead of once at the login size and again after.
            long t1 = System.nanoTime();                                              // TEMP timing
            stage.setTitle("Guess Market - " + userName);
            stage.setMinWidth(800);
            stage.setMinHeight(550);
            stage.setWidth(1200);
            stage.setHeight(700);
            stage.centerOnScreen();
            System.out.println("[timing] stage resize: " + (System.nanoTime() - t1) / 1_000_000 + " ms");

            long t2 = System.nanoTime();                                              // TEMP timing
            stage.getScene().setRoot(root);
            System.out.println("[timing] setRoot: " + (System.nanoTime() - t2) / 1_000_000 + " ms");
            FrameProbe.start("login", clickNanos);                                    // TEMP timing

        } catch (IOException e) {

            setLoading(false);
            showError("Could not load the main screen.");
            e.printStackTrace();
        }
    }


    private void setLoading(boolean loading) {

        userNameField.setDisable(loading);
        loginBtn.setDisable(loading);
    }

    private void showError(String message) {

        if (errorLabel == null) {
            return;
        }

        boolean hasError =
                message != null &&
                        !message.isBlank();

        errorLabel.setText(
                hasError ? message : ""
        );

        errorLabel.setVisible(hasError);
        errorLabel.setManaged(hasError);
    }
}