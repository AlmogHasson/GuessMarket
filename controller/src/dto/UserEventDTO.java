package dto;

import engine.EventTradingStatus;
import engine.Trade;

public record UserEventDTO(
        int eventId,
        String eventName,
        String role,
        int totalShares
) {

    public UserEventDTO(UserDTO user, EventTradingStatus event) {
        this(
                event.getId(),
                event.getName(),
                user.isEventMaker(event.getId()) ? "MM" : "Trader",
                event.getTradingHistory().stream()
                .filter(trade -> trade.getUserName().equals(user.getName()))
                .mapToInt(Trade::getSharesBought)
                .sum());
    }
}
