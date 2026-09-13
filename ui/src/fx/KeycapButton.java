package fx;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.function.BooleanSupplier;

public final class KeycapButton {

    private final Button button;
    private final BooleanSupplier enabled;
    private final DropShadow edge = new DropShadow();

    private Timeline movement;

    public KeycapButton(
            Button button,
            BooleanSupplier enabled
    ) {
        this.button = button;
        this.enabled = enabled;

        button.getStyleClass().add("keycap");

        // A sharp shadow creates the visible bottom edge.
        edge.setColor(Color.rgb(0, 0, 0, 0.45));
        edge.setRadius(0);
        edge.setSpread(1);
        edge.setOffsetX(0);
        edge.setOffsetY(button.isDisabled() ? 2 : 4);

        button.setEffect(edge);

        // Armed supports both mouse and keyboard activation.
        button.armedProperty().addListener(
                (observable, oldValue, newValue) -> refresh()
        );

        button.disabledProperty().addListener(
                (observable, oldValue, newValue) -> refresh()
        );
    }

    public void refresh() {
        if (movement != null) {
            movement.stop();
        }

        boolean pressed = button.isArmed() && !button.isDisabled();

        double faceY = pressed ? 3.0 : 0.0;

        double edgeY = button.isDisabled()
                ? 2.0
                : pressed ? 1.0 : 4.0;

        if (!enabled.getAsBoolean() || button.isDisabled()) {
            button.setTranslateY(0.0);
            edge.setOffsetY(button.isDisabled() ? 2.0 : 4.0);
            return;
        }

        movement = new Timeline(
                new KeyFrame(
                        Duration.millis(pressed ? 70 : 130),
                        new KeyValue(
                                button.translateYProperty(),
                                faceY,
                                Interpolator.EASE_OUT
                        ),
                        new KeyValue(
                                edge.offsetYProperty(),
                                edgeY,
                                Interpolator.EASE_OUT
                        )
                )
        );

        movement.play();
    }
}