package controllers;

import controllers.util.DialogHelper;
import javafx.animation.FadeTransition;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;

import util.HttpClientUtil;
import util.ThemeManager;

/** Controller for top.fxml - active user + logout, headline, animations toggle, theme selector. */
public class TopController {

    private MainController main;

    @FXML private Label       headline;
    @FXML private Label       activeUserLabel;
    @FXML private CheckBox    animationsCheckBox;
    @FXML private ComboBox<String> themeComboBox;
    @FXML private Button      logoutBtn;

    /** Self-contained setup only - main is not available yet. */
    @FXML
    public void initialize() {
        themeComboBox.getItems().setAll(ThemeManager.THEMES);
        themeComboBox.setValue(ThemeManager.getCurrent());
    }

    /** Called by MainController once every pane exists. */
    public void init(MainController main) {
        this.main = main;
        main.setAnimationsEnabled(animationsCheckBox.isSelected());

        // the label follows whoever the tester is currently acting as
        main.activeUserProperty()
                .addListener((obs, old, newSelection)
                        -> showActiveUser(newSelection == null? null : newSelection.name()));

        main.installKeycap(logoutBtn);
    }

    private void showActiveUser(String userName) {
        boolean none = userName == null || userName.isBlank();
        activeUserLabel.setText(none ? "No active user" : "Acting as: " + userName);
        activeUserLabel.getStyleClass().removeAll("active-user-none");
        if (none) {
            activeUserLabel.getStyleClass().add("active-user-none");
        }
    }

    // ---------------- logout ----------------

    /** The login screen is only shown once the server confirmed the logout. */
    @FXML
    void logout(ActionEvent event) {
        logoutBtn.setDisable(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                HttpClientUtil.logout();
                return null;
            }
        };

        task.setOnSucceeded(e -> main.showLoginScreen());

        task.setOnFailed(e -> {
            logoutBtn.setDisable(false);
            DialogHelper.showErrorAlert("Logout Failed", task.getException().getMessage());
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    // ---------------- animations + theme ----------------

    @FXML
    void toggleAnimations(ActionEvent event) {
        main.setAnimationsEnabled(animationsCheckBox.isSelected());
    }

    /**
     * Theme swap, honouring the animations toggle.
     * The progress bar is deliberately untouched by that flag - its motion is
     * feedback about work in progress, not decoration.
     */
    @FXML
    void setTheme(ActionEvent event) {
        if (main == null || themeComboBox.getValue() == null) {
            return;     // setValue() in initialize() fires this before init(main)
        }

        String selectedTheme = themeComboBox.getValue();
        var rootPane = main.getRootPane();

        if (!main.isAnimationsEnabled()) {
            ThemeManager.apply(rootPane.getScene(), selectedTheme);
            rootPane.setOpacity(1.0);          // in case a fade was interrupted
            return;
        }

        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), rootPane);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            try {
                ThemeManager.apply(rootPane.getScene(), selectedTheme);
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                FadeTransition fadeIn = new FadeTransition(Duration.millis(250), rootPane);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            }
        });
        fadeOut.play();
    }
}
