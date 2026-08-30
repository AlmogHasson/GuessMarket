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
    private String name;
    EventTradingStatus eventTradingStatus;


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
                : new OrderBook(event.getGMMethod().getGMOrderBook().getD());
        this.name = String.join(" ", event.getName());
        this.eventTradingStatus = new EventTradingStatus(
                this.name, this.options,
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

    public String getName() {
        return name;
    }

    public EventTradingStatus getEventTradingStatus() {
        return eventTradingStatus;
    }


    public Purchase participate(int optionNumber, int shares) {
        // assuming all parameters were validated beforehand

        Option option = options.get(optionNumber - 1);

        float beforeBalance = getBalance();

        // update shares
        option.buyShares(shares);

        float afterBalance = getBalance();

        // cost of the shares themselves
        float sharesCost = afterBalance - beforeBalance;

        // commission is charged only for on-purchase commission type
        float commissionCost = 0.0f;

        if (Objects.equals(comission.getType(), "on-purchase")) {
            commissionCost = sharesCost * comission.getValue() / 100;
        }

        float totalCost = sharesCost + commissionCost;

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

        event.updateHistory(new Trade(option.getOptionName(), shares, totalCost));

        return new Purchase(totalCost, sharesCost, commissionCost);
    }

    private void updateOptionsValues() {
        float firstOptionValue = method.calculateOptionValue(
                options.get(0).getTotalSharesBought(),
                options.get(1).getTotalSharesBought()
        );

        options.get(0).updateValue(firstOptionValue);
        options.get(1).updateValue(1 - firstOptionValue);
    }


    private float getBalance() {
        return method.calculateBalance(
                options.get(0).getTotalSharesBought(),
                options.get(1).getTotalSharesBought()
        );
    }

    public boolean isOpen() {
        return getEventTradingStatus().isOpen();
    }
}
