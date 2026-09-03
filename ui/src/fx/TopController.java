package fx;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.util.Objects;

/** Controller for top.fxml - headline, theme selector, animations toggle, file loading. */
public class TopController {

    private MainController main;

    @FXML private Label       headline;
    @FXML private CheckBox    animationsCheckBox;
    @FXML private ComboBox<String> themeComboBox;
    @FXML private Button      loadFileBtn;
    @FXML private Label       filePath;
    @FXML private ProgressBar progressBar;

    /** Self-contained setup only - main is not available yet. */
    @FXML
    public void initialize() {
        themeComboBox.getItems().setAll("Light", "Dark", "Japanese");
        themeComboBox.setValue("Dark");
        progressBar.setProgress(0);
    }

    /** Called by MainController once every pane exists. */
    public void init(MainController main) {
        this.main = main;
        main.setAnimationsEnabled(animationsCheckBox.isSelected());
    }

    // ---------------- file loading ----------------

    @FXML
    void loadFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open GuessMarket XML File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));

        File selectedFile = fileChooser.showOpenDialog(loadFileBtn.getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        String xmlFilePath = selectedFile.getAbsolutePath();

        Task<Void> task = createProgressTask(xmlFilePath);
        progressBar.progressProperty().bind(task.progressProperty());
        loadFileBtn.disableProperty().bind(task.runningProperty());

        task.setOnSucceeded(e -> {
            resetProgressBar();
            filePath.setText(xmlFilePath);

            main.onFileLoaded();

            PauseTransition hold = new PauseTransition(Duration.millis(400));
            hold.setOnFinished(done -> resetProgressBar());
            hold.play();
        });

        task.setOnFailed(e -> {
            resetProgressBar();
            DialogHelper.showErrorAlert("Load Failed", task.getException().getMessage());
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private Task<Void> createProgressTask(String xmlFilePath) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                for (int i = 0; i <= 100; i++) {
                    updateProgress(i, 100);
                    Thread.sleep(15);
                }
                main.getEngine().loadFile(xmlFilePath);
                return null;
            }
        };
    }

    /** Unbind before setting - a bound property throws on set(). */
    private void resetProgressBar() {
        progressBar.progressProperty().unbind();
        progressBar.setProgress(0);
        loadFileBtn.disableProperty().unbind();
        loadFileBtn.setDisable(false);
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
        String selectedTheme = themeComboBox.getValue().toLowerCase();
        String url = Objects.requireNonNull(
                getClass().getResource("themes/" + selectedTheme + ".css")).toExternalForm();

        var rootPane = main.getRootPane();

        if (!main.isAnimationsEnabled()) {
            rootPane.getScene().getStylesheets().setAll(url);
            rootPane.setOpacity(1.0);          // in case a fade was interrupted
            return;
        }

        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), rootPane);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            try {
                rootPane.getScene().getStylesheets().setAll(url);
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
