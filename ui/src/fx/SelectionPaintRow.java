package fx;

import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.css.converter.PaintConverter;
import javafx.geometry.Insets;
import javafx.scene.control.TableRow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class SelectionPaintRow<T> extends TableRow<T> {

    private static final CssMetaData<SelectionPaintRow<?>, Paint> BASE =
            paintMetadata("-slide-base-paint", row -> row.basePaint);

    private static final CssMetaData<SelectionPaintRow<?>, Paint> BAND =
            paintMetadata("-slide-band-paint", row -> row.bandPaint);

    private static final CssMetaData<SelectionPaintRow<?>, Paint> MIDPOINT =
            paintMetadata("-slide-midpoint-paint", row -> row.midpointPaint);

    private static final List<CssMetaData<? extends Styleable, ?>> CSS;

    static {
        List<CssMetaData<? extends Styleable, ?>> properties =
                new ArrayList<>(TableRow.getClassCssMetaData());

        properties.add(BASE);
        properties.add(BAND);
        properties.add(MIDPOINT);

        CSS = List.copyOf(properties);
    }

    private final StyleableObjectProperty<Paint> basePaint =
            new SimpleStyleableObjectProperty<>(
                    BASE, this, "basePaint", Color.TRANSPARENT
            );

    private final StyleableObjectProperty<Paint> bandPaint =
            new SimpleStyleableObjectProperty<>(
                    BAND, this, "bandPaint", Color.TRANSPARENT
            );

    private final StyleableObjectProperty<Paint> midpointPaint =
            new SimpleStyleableObjectProperty<>(
                    MIDPOINT, this, "midpointPaint", Color.TRANSPARENT
            );

    private static CssMetaData<SelectionPaintRow<?>, Paint> paintMetadata(
            String name,
            Function<SelectionPaintRow<?>,
                    StyleableObjectProperty<Paint>> property
    ) {
        return new CssMetaData<SelectionPaintRow<?>, Paint>(
                name,
                PaintConverter.getInstance(),
                Color.TRANSPARENT
        ) {
            @Override
            public boolean isSettable(SelectionPaintRow<?> row) {
                return !property.apply(row).isBound();
            }

            @Override
            public StyleableProperty<Paint> getStyleableProperty(
                    SelectionPaintRow<?> row
            ) {
                return property.apply(row);
            }
        };
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>>
    getControlCssMetaData() {
        return CSS;
    }

    public void paintBand(
            double topInset,
            double bottomInset,
            double lightness
    ) {
        Paint movingPaint = bandPaint.get();

        if (movingPaint instanceof Color original
                && midpointPaint.get() instanceof Color midpoint
                && midpoint.getOpacity() > 0.0) {

            Color target = new Color(
                    midpoint.getRed(),
                    midpoint.getGreen(),
                    midpoint.getBlue(),
                    original.getOpacity()
            );

            double blend = Math.clamp(lightness, 0.0, 1.0);

            movingPaint = original.interpolate(target, blend);
        }

        setBackground(
                new Background(
                        new BackgroundFill(
                                basePaint.get(),
                                CornerRadii.EMPTY,
                                Insets.EMPTY
                        ),
                        new BackgroundFill(
                                movingPaint,
                                CornerRadii.EMPTY,
                                new Insets(
                                        topInset,
                                        0,
                                        bottomInset,
                                        0
                                )
                        )
                )
        );
    }

    public void clearBand() {
        setBackground(
                new Background(
                        new BackgroundFill(
                                basePaint.get(),
                                CornerRadii.EMPTY,
                                Insets.EMPTY
                        )
                )
        );
    }
}