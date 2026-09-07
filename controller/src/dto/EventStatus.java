package dto;

public enum EventStatus {
    NOT_STARTED,
    OPEN,
    CLOSED;

    public static EventStatus from(engine.EventTradingStatus.Status engineStatus) {
        return switch (engineStatus) {
            case NOT_STARTED -> NOT_STARTED;
            case OPEN -> OPEN;
            case CLOSED -> CLOSED;
        };
    }
}