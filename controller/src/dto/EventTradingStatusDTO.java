package dto;

import java.util.List;
import engine.EventTradingStatus;

import static dto.EventStatus.from;

public record EventTradingStatusDTO(
        EventStatus status,
        String eventName,
        List<LMSROptionDTO> optionTradingStatus,
        double accountBalance, // for the user, not the event
        double totalCommissionPaid,
        List<TradeDTO> tradingHistory //history of trades for this event, for all users
    )
{
    // Constructor to create EventTradingStatusDTO from Event
    public EventTradingStatusDTO(EventTradingStatus ETS) {
        this(
            from(ETS.getStatus()),
            ETS.getName(),
            ETS.getOptionTradingStatuses().stream().map(LMSROptionDTO::new).toList(),
            ETS.getAccountBalance(),
            ETS.getTotalCommissionPaid(),
            ETS.getTradingHistory().stream().map(TradeDTO::new).toList()
        );
    }
}
