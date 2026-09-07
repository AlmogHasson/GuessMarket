package engine;

import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

public class EventTradingStatus implements Serializable {
    public enum Status {
        NOT_STARTED,
        OPEN,
        CLOSED
    }
    private static final long serialVersionUID = 1L;
        private int id;
        private String eventName;
        private Status status;
        private List<Option> options;
        private double accountBalance; // for the user, not the event
        private double totalCommissionPaid;
        private List<Trade> tradingHistory;

    public EventTradingStatus(int id, String eventName, List<Option> options, double accountBalance) {
        this.id = id;
        this.eventName = eventName;
        this.status = Status.NOT_STARTED;
        this.options = options;
        this.accountBalance = accountBalance;
        this.totalCommissionPaid = 0.0;
        this.tradingHistory = new ArrayList<Trade>();
    }

    // Getters
    public int getId() { return id; }


    public List<Option> getOptions() {
        return options;
    }

    public double getAccountBalance() {
        return accountBalance;
    }

    public double getTotalCommissionPaid() {
        return totalCommissionPaid;
    }

    public List<Trade> getTradingHistory() {
        return tradingHistory;
    }

    public List<Option> getOptionTradingStatuses() {
        return options;
    }

    public String getName() {
        return eventName;
    }
    
    public void updateAccountBalance(double newBalance) {
        this.accountBalance = newBalance;
    }

    public void updateTotalCommissionPaid(double newTotal) {
        this.totalCommissionPaid = newTotal;
    }

    public void updateHistory(Trade trade) {
        tradingHistory.add(trade);
    }

    public Status getStatus() {
        return status;
    }

    public boolean isOpen() {
        return status == Status.OPEN;
    }

    public void activate() {
        status = Status.OPEN;
    }

    public void close() {
        status = Status.CLOSED;
    }
}



