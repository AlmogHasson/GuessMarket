package engine;

import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

public class EventTradingStatus implements Serializable {
    private static final long serialVersionUID = 1L;

        private String eventName;
        private boolean isOpen;
        private List<Option> options;
        private double accountBalance; // for the user, not the event
        private double totalCommissionPaid;
        private List<Trade> tradingHistory;

    public EventTradingStatus(String eventName, List<Option> options, double accountBalance) {
        this.eventName = eventName;
        this.isOpen = true;
        this.options = options;
        this.accountBalance = accountBalance;
        this.totalCommissionPaid = 0.0;
        this.tradingHistory = new ArrayList<Trade>();
    }

    // Getters
    public boolean isOpen() {
        return isOpen;
    }

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

    public void close() {
        isOpen = false;
    }
}



