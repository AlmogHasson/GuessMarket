package dto;

import engine.Event;
import engine.Holding;

public record UserEventDTO(
        int eventId,
        String eventName,
        String role,
        EventStatus eventStatus,
        int totalShares,
        double investment,
        String methodType
) {

    public UserEventDTO(UserDTO user, Event event) {
        this(
                event.getId(),
                event.getEventName(),
                user.isEventMaker(event.getId()) ? "MM" : "Trader",
                EventStatus.from(event.getEventTradingStatus().getStatus()),
                sumShares(event, user.getName()),
                sumInvestment(event, user.getName()),
                event.getMethod() instanceof engine.OrderBook ? "order book" : "lmsr"
        );
    }

    public boolean isOrderBook() {
        return "order book".equals(methodType);
    }

    private static int sumShares(Event event, String userName) {
        Holding[] holdings = event.getUserHoldings(userName);
        if (holdings == null) return 0;
        int total = 0;
        for (Holding h : holdings) total += h.getShares();
        return total;
    }

    private static double sumInvestment(Event event, String userName) {
        Holding[] holdings = event.getUserHoldings(userName);
        if (holdings == null) return 0.0;
        double total = 0.0;
        for (Holding holding : holdings) total += holding.getTotalPaid();
        return total;
    }


}