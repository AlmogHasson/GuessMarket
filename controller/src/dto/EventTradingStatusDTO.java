package dto;

import java.util.List;
import engine.Event;

public record EventTradingStatusDTO(
        boolean isOpen,
        String eventName,
        List<OptionDTO> optionTradingStatus,
        float accountBalance, // for the user, not the event
        float totalCommissionPaid,
        List<TradeDTO> tradingHistory //history of trades for this event, for all users
    )
{
    // Constructor to create EventTradingStatusDTO from Event
    public EventTradingStatusDTO(Event event) {
        this(
            event.getEventTradingStatus().isOpen(),
            event.getName(),
            event.getEventTradingStatus().getOptionTradingStatuses().stream().map(OptionDTO::new).toList(),
            event.getEventTradingStatus().getAccountBalance(),
            event.getEventTradingStatus().getTotalCommissionPaid(),
            event.getEventTradingStatus().getTradingHistory().stream().map(TradeDTO::new).toList()
        );
    }
}
