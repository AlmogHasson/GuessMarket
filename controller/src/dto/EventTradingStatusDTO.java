package dto;

import java.util.List;
import engine.Event;

public record EventTradingStatusDTO(int eventId,
                                    String eventName,
                                    List<OptionTradingStatusDTO> options,
                                    double balance,
                                    double totalCommission,
                                    List<TradeDTO> tradingHistory,
                                    boolean closed,
                                    String winningOption) {

    // Constructor to create EventTradingStatusDTO from Event
    public EventTradingStatusDTO(Event event, double balance, double totalCommission,
                                 List<TradeDTO> tradingHistory, boolean closed, String winningOption) {
        this(
            event.getId(),
            event.getName(),
            event.getOptions().stream()
                .map(option -> new OptionTradingStatusDTO(
                    option.getOption(),
                    0.0,  // currentValue - you'll need to calculate this from LMSR
                    option.getSharesBought()
                ))
                .toList(),
            balance,
            totalCommission,
            tradingHistory,
            closed,
            winningOption
        );
    }
}

}
