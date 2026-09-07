package engine;

import generated.GMEvent;

import java.io.Serial;
import java.util.*;

import java.io.Serializable;

public class Event implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final int id;
    private final String description;
    private final Comission comission;
    private final List<Option> options;
    private final Method method;
    private final String eventName;
    private EventTradingStatus eventTradingStatus;
    //store the holdings of each user for this event, with the option number as the index in the array
    private final Map<String, Holding[]> userHoldings = new HashMap<>(); // [0]=option1, [1]=option2

    //get the event from schema and load it
    public Event(GMEvent event) {
        this.id = event.getId();
        this.description = event.getDescription();
        this.comission = new Comission(event.getCommission());
        this.options = new ArrayList<>();
        event.getGMOptions().getGMOption().forEach(option ->
                this.options.add(new Option(option, event.getGMOptions().getGMOption().indexOf(option) + 1))
        );
        this.method = event.getGMMethod().getGMLMSR() != null
                ? new LMSR(event.getGMMethod().getGMLMSR().getB())
                : new OrderBook(event.getGMMethod().getGMOrderBook());
        this.eventName = String.join(" ", event.getName());
        this.eventTradingStatus = new EventTradingStatus(
                this.id,
                this.eventName,
                this.options,
                0.0f
        );
    }


    //getters
    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Comission getComission() {
        return comission;
    }

    public List<Option> getOptions() {
        return options;
    }

    public Method getMethod() {
        return method;
    }

    public String getEventName() {
        return eventName;
    }

    public EventTradingStatus getEventTradingStatus() {
        return eventTradingStatus;
    }

    public void activate(User mm) {
        double charged = method.activate(mm, this);
        mm.setAccountBalance(mm.getAccountBalance() - charged);
        EventTradingStatus ets = getEventTradingStatus();
        ets.updateAccountBalance(ets.getAccountBalance() + charged);
        ets.activate(); // NOT_STARTED -> ACTIVE
    }

    public TradeResult participate(User user, Map<String, User> users, int optionNumber, int shares, Side side, Double price) {
        return method.executeTrade(this, users, new TradeRequest(user.getName(), optionNumber, shares, side, price));
    }



    /** validation is done in EngineImpl,
     so we can assume optionNumber and shares are valid and event is open for trading */
//    public Purchase participate(User user ,int optionNumber, int shares) {
//        Option option = options.get(optionNumber - 1);
//
//        double beforeBalance = getBalance();
//
//        // update shares
//        option.buyShares(shares);
//
//        double afterBalance = getBalance();
//
//        // cost of the shares themselves
//        double sharesCost = afterBalance - beforeBalance;
//
//        // commission is charged only for on-purchase commission type
//        double commissionCost = 0.0f;
//
//        if (Objects.equals(comission.getCommissionType(), "on-purchase")) {
//            commissionCost = sharesCost * comission.getValue() / 100;
//        }
//
//        double totalCost = sharesCost + commissionCost;
//
//        EventTradingStatus event = getEventTradingStatus();
//
//        // money received by the event: shares cost + commission if applicable
//        event.updateAccountBalance(
//                event.getAccountBalance() + totalCost
//        );
//
//        // update commission actually collected
//        event.updateTotalCommissionPaid(
//                event.getTotalCommissionPaid() + commissionCost
//        );
//
//        updateOptionsValues();
//
//        event.updateHistory(new Trade(user.getName(), option.getOptionName(), shares, totalCost));
//
//        return new Purchase(totalCost, sharesCost, commissionCost);
//    }

//    private void updateOptionsValues() {
//        double firstOptionValue = method.calculateOptionValue(
//                options.get(0).getTotalSharesBought(),
//                options.get(1).getTotalSharesBought()
//        );
//
//        options.get(0).updateValue(firstOptionValue);
//        options.get(1).updateValue(1 - firstOptionValue);
//    }
//
//
//    private double getBalance() {
//        return method.calculateBalance(
//                options.get(0).getTotalSharesBought(),
//                options.get(1).getTotalSharesBought()
//        );
//    }

    public boolean isOpen() {
        return getEventTradingStatus().isOpen();
    }

    public boolean isParticipating(String name) {
        return getEventTradingStatus().getTradingHistory().stream()
                .anyMatch(trade -> trade.getUserName().equals(name));
    }


    /** computeIfAbsent:
     *  If the key exists → returns the existing value
     *  If the key doesn't exist → calls the function to compute a value, stores it, and returns it
     */
    public Holding getOrCreateHolding(String userName, int optionNumber) {

        return userHoldings.computeIfAbsent(userName, k -> new Holding[]{new Holding(), new Holding()})
                [optionNumber - 1];
    }


    public Map<String, Holding[]> getUserHoldings() {
        return userHoldings;
    }
}
