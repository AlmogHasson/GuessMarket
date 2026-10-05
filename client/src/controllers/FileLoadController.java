package controllers;

import controllers.util.DialogHelper;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.FileChooser;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.util.Duration;
import util.HttpClientUtil;

import java.io.File;

/** Controller for fileLoad.fxml - the load-file row at the top of the Users tab. */
public class FileLoadController {

    private MainController main;

    @FXML private Button      loadFileBtn;
    @FXML private Label       filePath;
    @FXML private ProgressBar progressBar;

    /** Self-contained setup only - main is not available yet. */
    @FXML
    public void initialize() {
        progressBar.setProgress(0);
    }

    /** Called by MainController once every pane exists. */
    public void init(MainController main) {
        this.main = main;
        main.installKeycap(loadFileBtn);
    }

    @FXML
    void loadFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open GuessMarket XML File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));

        File selectedFile = fileChooser.showOpenDialog(loadFileBtn.getScene().getWindow());
        if (selectedFile == null) {
            return;
        }
        startUpload(selectedFile);
    }


    private void startUpload(File selectedFile) {
        loadFileBtn.setDisable(true);
        progressBar.setProgress(0);

        Timeline progressAnimation = createProgressAnimation();
        progressAnimation.play();

        Task<Void> uploadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                HttpClientUtil.uploadFile(selectedFile);
                return null;
            }
        };

        uploadTask.setOnSucceeded(event -> {
            progressAnimation.stop();

            finishProgressAnimation(() -> {
                filePath.setText(selectedFile.getAbsolutePath());
                loadFileBtn.setDisable(false);
                progressBar.setProgress(0);
                main.syncWithServer();
            });
        });

        uploadTask.setOnFailed(event -> {
            progressAnimation.stop();

            Throwable error = uploadTask.getException();
            loadFileBtn.setDisable(false);
            progressBar.setProgress(0);

            if (error instanceof HttpClientUtil.ServerException serverException) {
                DialogHelper.showErrorAlert("Load Failed", serverException.getMessage());
            } else {

                DialogHelper.showErrorAlert("Load Failed",
                        error == null || error.getMessage() == null
                                ? "Could not upload file."
                                : error.getMessage()
                );
            }
        });

        Thread thread = new Thread(uploadTask);
        thread.setDaemon(true);
        thread.start();
    }

    private Timeline createProgressAnimation() {
        return new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(progressBar.progressProperty(), 0)),
                new KeyFrame(Duration.seconds(2.5), new KeyValue(progressBar.progressProperty(), 0.8))
        );
    }

    private void finishProgressAnimation(Runnable onFinished) {

        Timeline finishAnimation =
                new Timeline(
                        new KeyFrame(
                                Duration.ZERO,
                                new KeyValue(
                                        progressBar.progressProperty(),
                                        progressBar.getProgress()
                                )
                        ),

                        new KeyFrame(
                                Duration.millis(250),
                                new KeyValue(
                                        progressBar.progressProperty(),
                                        1.0
                                )
                        )
                );

        finishAnimation.setOnFinished(event ->
                onFinished.run()
        );

        finishAnimation.play();
    }
}
