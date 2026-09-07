package engine;

import java.util.Map;

public interface Method {
    int getValue();

    /** MM funds the event. Returns the amount to debit from the MM's account. */
    double activate(User marketMaker, Event event);

    /** Process one trade request (LMSR: always a "buy" against the curve; OB: a limit order). */
    TradeResult executeTrade(Event event, Map<String, User> users, TradeRequest request);

    /** Settle the event: pay winners, handle commission, return any leftover to the MM if applicable. */
    void close(Event event, Map<String, User> users, int winningOptionNumber);
}