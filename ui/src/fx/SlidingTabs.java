package fx;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.util.function.BooleanSupplier;

public final class SlidingTabs {

    private final TabPane tabs;
    private final BooleanSupplier enabled;

    private final Region indicator = new Region();
    private final Runnable afterLayout = this::update;

    private Pane headerArea;
    private Tab previousTab;
    private Bounds target;
    private Timeline movement;

    public SlidingTabs(
            TabPane tabs,
            BooleanSupplier enabled
    ) {
        this.tabs = tabs;
        this.enabled = enabled;

        indicator.getStyleClass().add("sliding-tab-indicator");
        indicator.setManaged(false);
        indicator.setMouseTransparent(true);

        // The indicator is unmanaged, so animate its preferred size
        // and explicitly apply that size.
        indicator.prefWidthProperty().addListener(
                (observable, oldValue, newValue) ->
                        indicator.resize(
                                newValue.doubleValue(),
                                indicator.getHeight()
                        )
        );

        indicator.prefHeightProperty().addListener(
                (observable, oldValue, newValue) ->
                        indicator.resize(
                                indicator.getWidth(),
                                newValue.doubleValue()
                        )
        );

        tabs.sceneProperty().addListener(
                (observable, oldScene, newScene) ->
                        changeScene(oldScene, newScene)
        );

        changeScene(null, tabs.getScene());
    }

    private void changeScene(Scene oldScene, Scene newScene) {
        if (oldScene != null) {
            oldScene.removePostLayoutPulseListener(afterLayout);
        }

        if (newScene != null) {
            newScene.addPostLayoutPulseListener(afterLayout);
            javafx.application.Platform.requestNextPulse();
        } else if (movement != null) {
            movement.stop();
            target = null;
        }
    }

    private boolean attachIndicator() {
        Node areaNode = tabs.lookup(".tab-header-area");
        Node headersNode = tabs.lookup(
                ".tab-header-area > .headers-region"
        );

        if (!(areaNode instanceof Pane area)
                || headersNode == null) {
            return false;
        }

        if (headerArea == area && indicator.getParent() == area) {
            return true;
        }

        if (movement != null) {
            movement.stop();
        }

        if (indicator.getParent() instanceof Pane oldParent) {
            oldParent.getChildren().remove(indicator);
        }

        int headersIndex = area.getChildren().indexOf(headersNode);

        if (headersIndex < 0) {
            return false;
        }

        // Above the header background, below the tab labels.
        area.getChildren().add(headersIndex, indicator);

        headerArea = area;
        target = null;
        previousTab = null;

        if (!tabs.getStyleClass().contains("sliding-tabs")) {
            tabs.getStyleClass().add("sliding-tabs");
        }

        return true;
    }

    private void update() {
        if (!attachIndicator()) {
            return;
        }

        Node selectedHeader = tabs.lookup(
                ".tab-header-area > .headers-region > .tab:selected"
        );

        if (selectedHeader == null) {
            indicator.setVisible(false);
            target = null;
            return;
        }

        Bounds next = headerArea.sceneToLocal(
                selectedHeader.localToScene(
                        selectedHeader.getLayoutBounds()
                )
        );

        if (next.getWidth() <= 0 || next.getHeight() <= 0) {
            return;
        }

        indicator.setVisible(true);

        Tab selectedTab = tabs.getSelectionModel().getSelectedItem();

        boolean selectionChanged = selectedTab != previousTab;

        if (!selectionChanged && sameBounds(target, next)) {
            return;
        }

        boolean animate = target != null
                && selectionChanged
                && enabled.getAsBoolean();

        previousTab = selectedTab;
        target = next;

        if (movement != null) {
            movement.stop();
        }

        if (!animate) {
            place(next);
            return;
        }

        movement = new Timeline(
                new KeyFrame(
                        Duration.millis(250),
                        new KeyValue(
                                indicator.layoutXProperty(),
                                next.getMinX(),
                                Interpolator.EASE_BOTH
                        ),
                        new KeyValue(
                                indicator.layoutYProperty(),
                                next.getMinY(),
                                Interpolator.EASE_BOTH
                        ),
                        new KeyValue(
                                indicator.prefWidthProperty(),
                                next.getWidth(),
                                Interpolator.EASE_BOTH
                        ),
                        new KeyValue(
                                indicator.prefHeightProperty(),
                                next.getHeight(),
                                Interpolator.EASE_BOTH
                        )
                )
        );

        movement.play();
    }

    private boolean sameBounds(Bounds first, Bounds second) {
        return first != null
                && Math.abs(first.getMinX() - second.getMinX()) < 0.1
                && Math.abs(first.getMinY() - second.getMinY()) < 0.1
                && Math.abs(first.getWidth() - second.getWidth()) < 0.1
                && Math.abs(first.getHeight() - second.getHeight()) < 0.1;
    }

    private void place(Bounds bounds) {
        indicator.setPrefSize(
                bounds.getWidth(),
                bounds.getHeight()
        );

        indicator.resizeRelocate(
                bounds.getMinX(),
                bounds.getMinY(),
                bounds.getWidth(),
                bounds.getHeight()
        );
    }

    public void refreshMotion() {
        if (!enabled.getAsBoolean()) {
            if (movement != null) {
                movement.stop();
            }

            if (target != null) {
                place(target);
            }
        }
    }
}