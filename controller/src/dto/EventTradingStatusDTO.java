package dto;

import java.util.List;
import engine.Event;
import engine.EventTradingStatus;

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
    public EventTradingStatusDTO(EventTradingStatus ETS) {
        this(
            ETS.isOpen(),
            ETS.getName(),
            ETS.getOptionTradingStatuses().stream().map(OptionDTO::new).toList(),
            ETS.getAccountBalance(),
            ETS.getTotalCommissionPaid(),
            ETS.getTradingHistory().stream().map(TradeDTO::new).toList()
        );
    }
}
