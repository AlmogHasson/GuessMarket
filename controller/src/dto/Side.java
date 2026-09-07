package dto;

public enum Side {
    BUY, SELL;

    /** Converts an engine side to a DTO side. */
    public static Side fromEngine(engine.Side side) {
        return switch (side) {
            case BUY -> BUY;
            case SELL -> SELL;
        };
    }

    public engine.Side toEngine() {
        return switch (this) {
            case BUY -> engine.Side.BUY;
            case SELL -> engine.Side.SELL;
        };
    }

}