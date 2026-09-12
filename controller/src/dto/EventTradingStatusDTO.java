package dto;

import java.util.ArrayList;
import java.util.List;

import engine.Event;
import engine.EventTradingStatus;
import engine.LMSR;
import engine.Option;

import static dto.EventStatus.from;

public record EventTradingStatusDTO(
        EventStatus status,
        String eventName,
        List<OptionDTO> optionTradingStatus,
        double accountBalance, // for the user, not the event
        double totalCommissionPaid,
        List<TradeDTO> tradingHistory
    )
{
    // Constructor to create EventTradingStatusDTO from Event
    public EventTradingStatusDTO(Event event) {
        this(
            from(event.getEventTradingStatus().getStatus()),
                event.getEventTradingStatus().getName(),
                getOptionsForCtor(event),
                event.getEventTradingStatus().getAccountBalance(),
                event.getEventTradingStatus().getTotalCommissionPaid(),
                event.getEventTradingStatus().getTradingHistory().stream().map(TradeDTO::new).toList()
        );
    }

    private static List<OptionDTO> getOptionsForCtor(Event event) {
        List<OptionDTO> options = new ArrayList<>();

        if (event.getMethod() instanceof LMSR) {
            for (Option opt : event.getEventTradingStatus().getOptions()) {
                options.add(new LMSROptionDTO(opt));
            }
        } else {
            engine.OrderBook ob = (engine.OrderBook) event.getMethod();
            for (Option opt : event.getEventTradingStatus().getOptions()) {
                options.add(new OBOptionDTO(opt, ob.getRestingOrders(opt.getOptionNumber())));
            }
        }
        return options;
    }
}
