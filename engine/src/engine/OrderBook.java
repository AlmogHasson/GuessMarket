package engine;

import generated.GMOrderBook;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrderBook implements Method{
    private int d;
    protected int initial; //money the MM puts in the event - splits evenly between the two options
    protected boolean allowMint;
    private final OptionBook[] books = { new OptionBook(), new OptionBook() };
    private long orderSeq = 0;

    public OrderBook(GMOrderBook ob) {
        this.d = ob.getD();
        this.initial = ob.getInitial();
        this.allowMint = ob.getAllowMint().equals("true");
    }


    @Override
    public int getValue() {
        return d;
    }

    @Override
    public double activate(User mm, Event event) {
        int pairShares = initial / d;
        //add shares to the MM's holdings for both options, at a cost of initial/2 for each option
        event.getOrCreateHolding(mm.getName(), 1).applyBuy(pairShares, initial / 2.0);
        event.getOrCreateHolding(mm.getName(), 2).applyBuy(pairShares, initial / 2.0);
        //place orders for the MM to sell the initial shares at price d/2 for both options
        executeTrade(event, Map.of(mm.getName(), mm), new TradeRequest(mm.getName(), 1,  pairShares, Side.SELL,d / 2.0));
        executeTrade(event, Map.of(mm.getName(), mm), new TradeRequest(mm.getName(), 2,  pairShares, Side.SELL,d / 2.0));
        //update shares bought in the options to reflect the initial shares bought by the MM
        event.getOptions().get(0).buyShares(pairShares);
        event.getOptions().get(1).buyShares(pairShares); // total *outstanding* shares — only touched on activate/mint, never on ordinary fills

        //add trades to the event's trading history for the initial MM trades
        event.getEventTradingStatus().updateHistory(
                new Trade(mm.getName(), event.getOptions().get(0).getOptionName(), pairShares, initial / 2.0, Side.SELL, 0));
        event.getEventTradingStatus().updateHistory(
                new Trade(mm.getName(), event.getOptions().get(1).getOptionName(), pairShares, initial / 2.0, Side.SELL, 0));

        return initial;
    }

    @Override
    public TradeResult executeTrade(Event event, Map<String, User> users, TradeRequest req) {
        if (req.getPrice() == null || req.getPrice() >= d )
            throw new IllegalArgumentException("Invalid price for Order Book order: " + req.getPrice() + " (must be < d = " + d + ")");

        Order incoming =
                new Order(++orderSeq, req.getUserName(), req.getSide(), req.getShares(), req.getPrice(), orderSeq);

        List<Trade> trades = new ArrayList<>();
        matchSameOption(event, users, incoming, req.getOptionNumber(), trades);

        if (allowMint && incoming.getQuantity() > 0 && req.getSide() == Side.BUY)
            matchMint(event, users, incoming, req.getOptionNumber(), trades);

        if (incoming.getQuantity() > 0)
            books[req.getOptionNumber() - 1].rest(incoming);

        double net = trades.stream().filter(t -> t.getUserName().equals(req.getUserName()))
                .mapToDouble(t -> t.getSide() == Side.BUY ? t.getPricePaid() : -t.getPricePaid()).sum();

        double commission = trades.stream().filter(t -> t.getUserName().equals(req.getUserName()))
                .mapToDouble(Trade::getCommission).sum();

        return new TradeResult(net, commission, trades, incoming.getQuantity());
    }


    @Override
    public void close( Event event, Map<String, User> users, int winningOptionNumber) {
        boolean onClose = "on-close".equals(event.getComission().getCommissionType());
        double percentage = event.getComission().getValue() / 100.0;

        for (var entry : event.getUserHoldings().entrySet()) {
            Holding holding = entry.getValue()[winningOptionNumber - 1];

            if (holding.getShares() <= 0) {
                continue;
            }

            double gross = holding.getShares() * d;
            double commission = onClose ? gross * percentage : 0.0;
            event.getEventTradingStatus().updateTotalCommissionPaid(
                    event.getEventTradingStatus().getTotalCommissionPaid() + commission
            );
            double net = gross - commission;

            User user = users.get(entry.getKey());

            // pay winner
            user.setAccountBalance(user.getAccountBalance() + net);

            // money leaves event contract
            event.getEventTradingStatus().updateAccountBalance(
                    event.getEventTradingStatus().getAccountBalance() - gross
            );

            // commission goes to MM
            if (commission > 0) {
                User mm = users.values().stream().filter(u -> u.isEventMaker(event.getId()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Market maker not found"));
                mm.setAccountBalance(mm.getAccountBalance() + commission);
            }
        }

        event.getEventTradingStatus().close();
    }



    public int getInitial() {
        return initial;
    }

    public boolean getAllowMint() {
        return allowMint;
    }

    public void setInitial(int value) {
        this.initial = value;
    }

    public void setD(int value) {
        this.d = value;
    }

    public void setAllowMint(boolean value) {
        this.allowMint = value;
    }

    private void matchSameOption(Event event, Map<String,User> users, Order incoming, int optNum, List<Trade> trades) {
        OptionBook book = books[optNum - 1];
//        String optName = event.getOptions().get(optNum - 1).getOptionName();
        while (incoming.getQuantity() > 0) {
            Order resting = book.bestOpposing(incoming.getSide()); // best ask if incoming is BUY, best bid if SELL
            if (resting == null) break;
            boolean crosses = incoming.getSide() == Side.BUY
                    ? incoming.getPrice() >= resting.getPrice()
                    : incoming.getPrice() <= resting.getPrice();
            if (!crosses) break;

            int qty = Math.min(incoming.getQuantity(), resting.getQuantity());
            double price = resting.getPrice(); // resting order sets the execution price
            String buyerName  = incoming.getSide() == Side.BUY ? incoming.getUserName() : resting.getUserName();
            String sellerName = incoming.getSide() == Side.BUY ? resting.getUserName() : incoming.getUserName();
            fill(event, users, buyerName, sellerName, optNum, qty, price, trades);
            incoming.reduce(qty);
            resting.reduce(qty);
            book.updateLast(price);
            if (resting.getQuantity() == 0) book.removeResting(resting);
        }
    }

    private void fill(Event event, Map<String, User> users, String buyerName, String sellerName,
                      int optNum, int qty, double price, List<Trade> trades) {
        String optName = event.getOptions().get(optNum - 1).getOptionName();
        double gross = qty * price;
        double commission = "on-purchase".equals(event.getComission().getCommissionType())
                ? gross * event.getComission().getValue() / 100.0 : 0.0;

        User buyer = users.get(buyerName);
        buyer.setAccountBalance(buyer.getAccountBalance() - (gross + commission));
        event.getOrCreateHolding(buyerName, optNum).applyBuy(qty, gross + commission);

        User seller = users.get(sellerName);
        seller.setAccountBalance(seller.getAccountBalance() + gross);
        event.getOrCreateHolding(sellerName, optNum).applySell(qty, gross);

        event.getOptions().get(optNum - 1).updateValue(price);

        EventTradingStatus ets = event.getEventTradingStatus();
        ets.updateTotalCommissionPaid(ets.getTotalCommissionPaid() + commission);

        Trade buyerTrade = new Trade(buyerName, optName, qty, gross + commission, Side.BUY, commission);
        Trade sellerTrade = new Trade(sellerName, optName, qty, gross, Side.SELL, 0);
        trades.add(buyerTrade);
        trades.add(sellerTrade);
        ets.updateHistory(buyerTrade);
        ets.updateHistory(sellerTrade);
    }

    private void matchMint(Event event, Map<String,User> users, Order incoming, int optNum, List<Trade> trades) {
        int otherOptNum = 3 - optNum;
        OptionBook otherBook = books[otherOptNum - 1];
        while (incoming.getQuantity() > 0) {
            Order restingBid = otherBook.bestBidOrder();
            if (restingBid == null || incoming.getPrice() + restingBid.getPrice() < d)
                break;

            int qty = Math.min(incoming.getQuantity(), restingBid.getQuantity());
            double restingPay = restingBid.getPrice();
            double incomingPay = d - restingPay;

            mintFill(event, users, restingBid.getUserName(), otherOptNum, qty, restingPay, trades);
            mintFill(event, users, incoming.getUserName(), optNum, qty, incomingPay, trades);
            event.getOptions().get(optNum - 1).buyShares(qty);
            event.getOptions().get(otherOptNum - 1).buyShares(qty);
            event.getEventTradingStatus().updateAccountBalance(event.getEventTradingStatus().getAccountBalance() + d * qty);

            incoming.reduce(qty);
            restingBid.reduce(qty);
            if (restingBid.getQuantity() == 0) otherBook.removeResting(restingBid);
        }
    }

    private void mintFill(Event event, Map<String, User> users, String buyerName,
                          int optNum, int qty, double price, List<Trade> trades) {
        String optName = event.getOptions().get(optNum - 1).getOptionName();
        double gross = qty * price;
        double commission = "on-purchase".equals(event.getComission().getCommissionType())
                ? gross * event.getComission().getValue() / 100.0 : 0.0;

        User buyer = users.get(buyerName);
        buyer.setAccountBalance(buyer.getAccountBalance() - (gross + commission));
        event.getOrCreateHolding(buyerName, optNum).applyBuy(qty, gross + commission);

        event.getOptions().get(optNum - 1).updateValue(price);

        EventTradingStatus ets = event.getEventTradingStatus();
        ets.updateTotalCommissionPaid(ets.getTotalCommissionPaid() + commission);

        Trade trade = new Trade(buyerName, optName, qty, gross + commission, Side.BUY, commission);
        trades.add(trade);
        ets.updateHistory(trade);
    }


    public List<Order> getRestingOrders(int optionNumber) {
        return books[optionNumber - 1].getRestingOrders();
    }

    public int getReservedSellShares(String userName, int optionNumber) {
        return books[optionNumber - 1]
                .getRestingOrders()
                .stream()
                .filter(order -> order.getSide() == Side.SELL)
                .filter(order -> order.getUserName().equals(userName))
                .mapToInt(Order::getQuantity)
                .sum();
    }
}
