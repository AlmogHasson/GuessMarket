package fx;


import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.animation.Interpolator;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

public final class RowAnimations {

    private final BooleanSupplier enabled;
    private final Map<TableView<?>, Tracker<?>> trackers =
            new IdentityHashMap<>();

    public RowAnimations(BooleanSupplier enabled) {
        this.enabled = enabled;
    }

    public <T> void update(
            TableView<T> table,
            Object scope,
            Collection<T> items,
            Function<T, ?> keyFactory
    ) {
        update(table, scope, items, keyFactory, true);
    }

    public <T> void update(
            TableView<T> table,
            Object scope,
            Collection<T> items,
            Function<T, ?> keyFactory,
            boolean animate
    ) {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException(
                    "Table updates must run on the JavaFX Application Thread."
            );
        }

        Objects.requireNonNull(scope, "scope");

        @SuppressWarnings("unchecked")
        Tracker<T> tracker = (Tracker<T>) trackers.computeIfAbsent(
                table,
                ignored -> new Tracker<>(table, keyFactory, enabled)
        );

        tracker.show(scope, items, animate);
    }

    public void reset() {
        trackers.values().forEach(Tracker::reset);
    }

    private record RowKey(Object scope, Object id) {
    }

    private static final class Tracker<T> {

        // First appearance.
        private static final long SLOW_FADE = 500_000_000L;
        private static final long SLOW_GAP = 80_000_000L;
        private static final long SLOW_BATCH = 1_500_000_000L;

        // Subsequent appearances and refreshes.
        private static final long FAST_FADE = 220_000_000L;
        private static final long FAST_GAP = 30_000_000L;
        private static final long FAST_BATCH = 600_000_000L;

        private final TableView<T> table;
        private final Function<T, ?> keyFactory;
        private final BooleanSupplier enabled;
        private boolean animateUpdate = true;

        // Each table remembers which user/event views it has displayed.
        private final Set<Object> displayedScopes = new HashSet<>();

        // Start times remain attached to stable IDs during row recycling.
        private final Map<RowKey, Long> started = new HashMap<>();

        private Object scope;
        private boolean refreshPending;
        private long fadeDuration = SLOW_FADE;

        private final Runnable afterLayout = this::flush;

        private final AnimationTimer clock = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Opacity is applied after layout in flush().
            }
        };

        private Tracker(
                TableView<T> table,
                Function<T, ?> keyFactory,
                BooleanSupplier enabled
        ) {
            this.table = table;
            this.keyFactory = keyFactory;
            this.enabled = enabled;

            table.setRowFactory(ignored -> new TableRow<>());

            selectionSlide = new SlidingRowSelection<>(
                    table,
                    () -> scope,
                    keyFactory,
                    enabled
            );

            table.sceneProperty().addListener(
                    (observable, oldScene, newScene) ->
                            changeScene(oldScene, newScene)
            );

            table.visibleProperty().addListener(
                    (observable, wasVisible, isVisible) -> {
                        if (isVisible) {
                            refreshPending = true;
                            clock.start();
                        }
                    }
            );

            changeScene(null, table.getScene());
        }

        private final SlidingRowSelection<T> selectionSlide;


        private void changeScene(Scene oldScene, Scene newScene) {
            if (oldScene != null) {
                oldScene.removePostLayoutPulseListener(afterLayout);
            }

            if (newScene != null) {
                newScene.addPostLayoutPulseListener(afterLayout);
                refreshPending = true;
                clock.start();
            } else {
                clock.stop();
            }
        }

        private void show(Object nextScope, Collection<T> values, boolean animate) {
            List<T> snapshot = List.copyOf(values);
            Set<Object> ids = new HashSet<>();

            for (T item : snapshot) {
                Object id = Objects.requireNonNull(
                        keyFactory.apply(item),
                        "Row IDs must not be null."
                );

                if (!ids.add(id)) {
                    throw new IllegalArgumentException(
                            "Duplicate row ID in one table: " + id
                    );
                }
            }

            scope = nextScope;
            animateUpdate = animate;

            // Every update requests an animation.
            // Several updates before rendering are combined into one.
            refreshPending = true;

            table.getItems().setAll(snapshot);
            clock.start();
        }

        private void flush() {
            if (scope == null
                    || table.getScene() == null
                    || !isTreeVisible(table)
                    || table.getWidth() <= 0
                    || table.getHeight() <= 0) {
                clock.stop();
                return;
            }

            List<TableRow<?>> rows = new java.util.ArrayList<>();

            for (Node node : table.lookupAll(".table-row-cell")) {
                if (node instanceof TableRow<?> row
                        && row.getTableView() == table) {

                    if (row.isEmpty() || row.getItem() == null) {
                        row.setOpacity(1.0);
                    } else {
                        rows.add(row);
                    }
                }
            }

            rows.sort(
                    java.util.Comparator.comparingInt(
                            IndexedCell::getIndex
                    )
            );

            if (!animateUpdate || !enabled.getAsBoolean()) {
                started.clear();
                refreshPending = false;

                for (TableRow<?> row : rows) {
                    row.setOpacity(1.0);
                }

                if (!rows.isEmpty()) {
                    displayedScopes.add(scope);
                }

                clock.stop();
                return;
            }

            if (rows.isEmpty()) {
                started.clear();

                if (table.getItems().isEmpty()) {
                    clock.stop();
                }

                // Keep the request until actual rows can be displayed.
                return;
            }

            long now = System.nanoTime();

            if (refreshPending) {
                refreshPending = false;
                started.clear();

                boolean firstAppearance = displayedScopes.add(scope);

                fadeDuration = firstAppearance
                        ? SLOW_FADE
                        : FAST_FADE;

                long preferredGap = firstAppearance
                        ? SLOW_GAP
                        : FAST_GAP;

                long batchDuration = firstAppearance
                        ? SLOW_BATCH
                        : FAST_BATCH;

                long gap = rows.size() <= 1
                        ? 0L
                        : Math.min(
                        preferredGap,
                        Math.max(0L, batchDuration - fadeDuration)
                                / (rows.size() - 1)
                );

                for (int i = 0; i < rows.size(); i++) {
                    TableRow<?> row = rows.get(i);

                    @SuppressWarnings("unchecked")
                    T item = (T) row.getItem();

                    RowKey key = new RowKey(
                            scope,
                            keyFactory.apply(item)
                    );

                    started.put(key, now + i * gap);
                }
            }

            started.entrySet().removeIf(entry ->
                    now - entry.getValue() >= fadeDuration
            );

            for (TableRow<?> row : rows) {
                @SuppressWarnings("unchecked")
                T item = (T) row.getItem();

                RowKey key = new RowKey(
                        scope,
                        keyFactory.apply(item)
                );

                Long start = started.get(key);

                if (start == null) {
                    row.setOpacity(1.0);
                    continue;
                }

                double progress = Math.clamp(
                        (double) (now - start) / fadeDuration
                        ,
                        0.0,
                        1.0);

                row.setOpacity(
                        Interpolator.EASE_OUT.interpolate(
                                0.0,
                                1.0,
                                progress
                        )
                );
            }

            if (started.isEmpty()) {
                clock.stop();
            } else {
                clock.start();
            }
        }

        private void reset() {
            selectionSlide.reset();
            displayedScopes.clear();
            started.clear();
            scope = null;
            refreshPending = false;
            clock.stop();

            for (Node node : table.lookupAll(".table-row-cell")) {
                if (node instanceof TableRow<?> row
                        && row.getTableView() == table) {
                    row.setOpacity(1.0);
                }
            }

            table.refresh();
        }
    }

    private static boolean isTreeVisible(Node node) {
        for (Node current = node;
             current != null;
             current = current.getParent()) {
            if (!current.isVisible()) {
                return false;
            }
        }

        return true;
    }


}