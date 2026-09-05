package engine;

import generated.GMEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import java.io.Serializable;

public class Event implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String description;
    private Comission comission;
    private List<Option> options;
    private Method method;
    private String eventName;
    private EventTradingStatus eventTradingStatus;

    //get the event from schema and load it
    public Event(GMEvent event) {
        this.id = event.getId();
        this.description = event.getDescription();
        this.comission = new Comission(event.getCommission());
        this.options = new ArrayList<>();
        event.getGMOptions().getGMOption().forEach(option ->
                this.options.add(new Option(option))
        );
        this.method = event.getGMMethod().getGMLMSR() != null
                ? new LMSR(event.getGMMethod().getGMLMSR().getB())
                : new OrderBook(event.getGMMethod().getGMOrderBook());
        this.eventName = String.join(" ", event.getName());
        this.eventTradingStatus = new EventTradingStatus(
                this.id,
                this.eventName, this.options,
                method.calculateBalance(
                        options.get(0).getTotalSharesBought(),
                        options.get(1).getTotalSharesBought())
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


    /** validation is done in EngineImpl,
     so we can assume optionNumber and shares are valid and event is open for trading */
    public Purchase participate(User user ,int optionNumber, int shares) {
        Option option = options.get(optionNumber - 1);

        double beforeBalance = getBalance();

        // update shares
        option.buyShares(shares);

        double afterBalance = getBalance();

        // cost of the shares themselves
        double sharesCost = afterBalance - beforeBalance;

        // commission is charged only for on-purchase commission type
        double commissionCost = 0.0f;

        if (Objects.equals(comission.getCommissionType(), "on-purchase")) {
            commissionCost = sharesCost * comission.getValue() / 100;
        }

        double totalCost = sharesCost + commissionCost;

        EventTradingStatus event = getEventTradingStatus();

        // money received by the event: shares cost + commission if applicable
        event.updateAccountBalance(
                event.getAccountBalance() + totalCost
        );

        // update commission actually collected
        event.updateTotalCommissionPaid(
                event.getTotalCommissionPaid() + commissionCost
        );

        updateOptionsValues();

        event.updateHistory(new Trade(user.getName(), option.getOptionName(), shares, totalCost));

        return new Purchase(totalCost, sharesCost, commissionCost);
    }

    private void updateOptionsValues() {
        double firstOptionValue = method.calculateOptionValue(
                options.get(0).getTotalSharesBought(),
                options.get(1).getTotalSharesBought()
        );

        options.get(0).updateValue(firstOptionValue);
        options.get(1).updateValue(1 - firstOptionValue);
    }


    private double getBalance() {
        return method.calculateBalance(
                options.get(0).getTotalSharesBought(),
                options.get(1).getTotalSharesBought()
        );
    }

    public boolean isOpen() {
        return getEventTradingStatus().isOpen();
    }

    public boolean isParticipating(String name) {
        return getEventTradingStatus().getTradingHistory().stream()
                .anyMatch(trade -> trade.getUserName().equals(name));
    }
}
