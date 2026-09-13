package fx;

import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

public final class SlidingRowSelection<T> {

    private static final long DURATION = 200_000_000L;

    private static final PseudoClass COVERED =
            PseudoClass.getPseudoClass("selection-band");

    private final TableView<T> table;
    private final Supplier<Object> scope;
    private final Function<T, ?> keyFactory;
    private final BooleanSupplier enabled;

    private final Set<TableRow<?>> paintedRows = new HashSet<>();

    private final Runnable afterLayout = this::update;

    private final AnimationTimer clock = new AnimationTimer() {
        @Override
        public void handle(long now) {
            // Drawing happens after layout.
        }
    };

    private Object previousScope;
    private Object previousKey;

    private double y;
    private double fromY;
    private double targetY;
    private double bandHeight;

    private long startTime;
    private boolean moving;
    private double bandLightness;

    public SlidingRowSelection(
            TableView<T> table,
            Supplier<Object> scope,
            Function<T, ?> keyFactory,
            BooleanSupplier enabled
    ) {
        this.table = table;
        this.scope = scope;
        this.keyFactory = keyFactory;
        this.enabled = enabled;

        // Your display-only tables remain unchanged.
        if (table.getSelectionModel() == null) {
            return;
        }
        table.setRowFactory(ignored -> new SelectionPaintRow<T>());

        table.getStyleClass().add("sliding-row-selection");

        table.sceneProperty().addListener(
                (observable, oldScene, newScene) ->
                        changeScene(oldScene, newScene)
        );

        changeScene(null, table.getScene());
    }

    private void changeScene(Scene oldScene, Scene newScene) {
        if (oldScene != null) {
            oldScene.removePostLayoutPulseListener(afterLayout);
        }

        reset();

        if (newScene != null) {
            newScene.addPostLayoutPulseListener(afterLayout);
            javafx.application.Platform.requestNextPulse();
        }
    }

    private void update() {
        if (table.getScene() == null || !isVisibleInTree()) {
            reset();
            return;
        }

        var selection = table.getSelectionModel();

        if (selection == null
                || selection.getSelectedItem() == null
                || selection.getSelectedIndex() < 0) {
            reset();
            return;
        }

        List<TableRow<?>> rows = new ArrayList<>();
        TableRow<?> selectedRow = null;

        for (Node node : table.lookupAll(".table-row-cell")) {
            if (node instanceof TableRow<?> row
                    && row.getTableView() == table) {
                rows.add(row);

                if (!row.isEmpty()
                        && row.getIndex() == selection.getSelectedIndex()) {
                    selectedRow = row;
                }
            }
        }

        // The selected row may be outside the materialized viewport.
        if (selectedRow == null) {
            reset();
            return;
        }

        Bounds selectedBounds = boundsInTable(selectedRow);

        if (selectedBounds.getHeight() <= 0) {
            reset();
            return;
        }

        Object currentScope = scope.get();
        Object currentKey = keyFactory.apply(selection.getSelectedItem());

        boolean sameScope = Objects.equals(
                previousScope,
                currentScope
        );

        boolean sameSelection = sameScope
                && Objects.equals(previousKey, currentKey);

        long now = System.nanoTime();
        double nextY = selectedBounds.getMinY();
        double nextHeight = selectedBounds.getHeight();

        if (!sameSelection) {
            boolean canSlide = previousKey != null
                    && sameScope
                    && enabled.getAsBoolean();

            fromY = y;
            targetY = nextY;
            bandHeight = nextHeight;
            startTime = now;
            moving = canSlide;

            previousScope = currentScope;
            previousKey = currentKey;
        } else if (Math.abs(nextY - targetY) > 0.1
                || Math.abs(nextHeight - bandHeight) > 0.1) {
            // Follow scrolling, sorting and resizing immediately.
            targetY = nextY;
            bandHeight = nextHeight;
            moving = false;
        }

        if (!enabled.getAsBoolean()) {
            moving = false;
        }

        if (moving) {
            double progress = Math.min(
                    1.0,
                    (double) (now - startTime) / DURATION
            );

            y = Interpolator.EASE_BOTH.interpolate(
                    fromY,
                    targetY,
                    progress
            );

            // Original color → lighter halfway through → original color.
            bandLightness = Math.sin(Math.PI * progress);

            if (progress >= 1.0) {
                moving = false;
                bandLightness = 0.0;
            }
        } else {
            y = targetY;
            bandLightness = 0.0;
        }

        // Restore rows that the virtualized table has removed.
        for (TableRow<?> row :
                new ArrayList<>(paintedRows)) {
            if (!rows.contains(row)) {
                restore(row);
            }
        }

        for (TableRow<?> row : rows) {
            if (row.isEmpty() || row.getItem() == null) {
                restore(row);
                continue;
            }

            Bounds bounds = boundsInTable(row);

            double top = Math.max(
                    0.0,
                    y - bounds.getMinY()
            );

            double bottom = Math.min(
                    bounds.getHeight(),
                    y + bandHeight - bounds.getMinY()
            );

            if (bottom <= top) {
                restore(row);
                continue;
            }

            paint(
                    row,
                    top,
                    bounds.getHeight() - bottom
            );

            // Switch text color when the band crosses the row's center.
            double center = bounds.getHeight() / 2.0;

            row.pseudoClassStateChanged(
                    COVERED,
                    top <= center && bottom >= center
            );
        }

        if (moving) {
            clock.start();
        } else {
            clock.stop();
        }
    }

    private Bounds boundsInTable(TableRow<?> row) {
        return table.sceneToLocal(
                row.localToScene(row.getLayoutBounds())
        );
    }

    private void paint(
            TableRow<?> row,
            double topInset,
            double bottomInset
    ) {
        if (row instanceof SelectionPaintRow<?> painted) {
            paintedRows.add(row);
            painted.paintBand(topInset, bottomInset, bandLightness);
        }
    }

    private void restore(TableRow<?> row) {
        paintedRows.remove(row);

        if (row instanceof SelectionPaintRow<?> painted) {
            painted.clearBand();
        }

        row.pseudoClassStateChanged(COVERED, false);
    }

    private boolean isVisibleInTree() {
        for (Node node = table;
             node != null;
             node = node.getParent()) {
            if (!node.isVisible()) {
                return false;
            }
        }

        return true;
    }

    public void reset() {
        clock.stop();
        moving = false;
        previousScope = null;
        previousKey = null;

        for (TableRow<?> row :
                new ArrayList<>(paintedRows)) {
            restore(row);
        }
    }
}