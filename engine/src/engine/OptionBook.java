package engine;

import java.util.*;

public class OptionBook {

    private final TreeMap<Double, LinkedList<Order>> bids = new TreeMap<>(Comparator.reverseOrder());
    private final TreeMap<Double, LinkedList<Order>> asks = new TreeMap<>();
    private Double lastTradePrice;

    public void rest(Order o) {
        (o.getSide() == Side.BUY ? bids : asks)
                .computeIfAbsent(o.getPrice(), _ -> new LinkedList<>()).add(o);
    }

    public Order bestOpposing(Side incomingSide) {
        /* first order in asks (if BUY) or bids (if SELL), FIFO within price */
        return (incomingSide == Side.BUY) ? bestAskOrder() : bestBidOrder();
    }
    public Order bestBidOrder() {
        /* head of best-priced bid queue, or null */
        return bids.isEmpty() ? null : bids.firstEntry().getValue().peekFirst();
    }
    public Order bestAskOrder() {
        /* head of best-priced ask queue, or null */
        return asks.isEmpty() ? null : asks.firstEntry().getValue().peekFirst();
    }

    public void removeResting(Order order) {
        /* remove from the map/queue it lives in */
        TreeMap<Double, LinkedList<Order>> book = (order.getSide() == Side.BUY) ? bids : asks;
        book.get(order.getPrice()).remove(order);
        if (book.get(order.getPrice()).isEmpty()) {
            book.remove(order.getPrice());
        }
    }

    public void updateLast(double p) { lastTradePrice = p; }

    public Double bestBid() { return bids.isEmpty() ? null : bids.firstKey(); }
    public Double bestAsk() { return asks.isEmpty() ? null : asks.firstKey(); }
    public Double mid()     { return (bestBid()==null||bestAsk()==null) ? null : (bestBid()+bestAsk())/2; }
    public Double spread()  { return (bestBid()==null||bestAsk()==null) ? null : bestAsk()-bestBid(); }
    public Double last()    { return lastTradePrice; }

    public List<Order> getRestingOrders() {
        List<Order> all = new ArrayList<>();
        bids.values().forEach(all::addAll);
        asks.values().forEach(all::addAll);
        return all;
    }
}
