package controllers.util;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class DialogHelper {

    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public static void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Error");
        alert.setContentText(message);
        applyTheme(alert);
        alert.showAndWait();
    }

    public static void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyTheme(alert);
        alert.showAndWait();
    }

    /** Pick-one popup (e.g. the winning option when closing an event), themed. */
    public static Optional<String> showChoiceDialog(String title, String header,
                                                    String label, List<String> options) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(options.getFirst(), options);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(label);
        applyTheme(dialog);
        return dialog.showAndWait();
    }

    /**
     * Popup asking how much to deposit, themed like the owner window.
     * The Deposit button stays disabled until the text is a positive amount
     * with at most two decimals, so the caller only ever receives a valid
     * number. Cancel / closing the popup returns empty.
     */
    public static Optional<Double> showDepositDialog(Window owner) {
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Deposit");
        dialog.setHeaderText("Load funds into your account");
        applyTheme(dialog, owner);
        dialog.getDialogPane().setPrefWidth(350);
        dialog.getDialogPane().setMinHeight(220);
        dialog.setResizable(true);   // lets the user drag it bigger

        ButtonType depositType = new ButtonType("Deposit", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(depositType, ButtonType.CANCEL);

        TextField field = new TextField();
        field.setPromptText("0.00");

        Label hint = new Label();
        hint.getStyleClass().add("muted-label");

        VBox content = new VBox(12, new Label("Amount"), field, hint);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);

        Node depositBtn = dialog.getDialogPane().lookupButton(depositType);
        depositBtn.setDisable(true);

        field.textProperty().addListener((obs, old, text) -> {
            boolean valid = parseAmount(text) != null;
            depositBtn.setDisable(!valid);
            hint.setText(text.isBlank() || valid
                    ? "" : "Enter a positive amount with up to 2 decimals.");
        });

        Platform.runLater(field::requestFocus);

        dialog.setResultConverter(button ->
                button == depositType ? parseAmount(field.getText()) : null);

        return dialog.showAndWait();
    }

    /** Themes a dialog like whichever application window is in front. */
    private static void applyTheme(Dialog<?> dialog) {
        applyTheme(dialog, findOwner());
    }

    /**
     * Makes the dialog a child of the owner window (so it opens centred on it)
     * and gives it the owner's stylesheets, i.e. the current theme.
     */
    private static void applyTheme(Dialog<?> dialog, Window owner) {
        if (owner == null || owner.getScene() == null) {
            return;
        }
        dialog.initOwner(owner);
        dialog.getDialogPane().getStylesheets().addAll(owner.getScene().getStylesheets());
    }

    /** The focused window if there is one, otherwise any showing window. */
    private static Window findOwner() {
        List<Window> showing = Window.getWindows().stream()
                .filter(Window::isShowing)
                .toList();
        return showing.stream()
                .filter(Window::isFocused)
                .findFirst()
                .orElse(showing.isEmpty() ? null : showing.getFirst());
    }

    /** Returns the amount, or null if the text is not a positive amount. */
    private static Double parseAmount(String text) {
        String t = text == null ? "" : text.trim();
        if (!AMOUNT.matcher(t).matches()) {
            return null;
        }
        double value = Double.parseDouble(t);
        return value > 0 ? value : null;
    }

}
