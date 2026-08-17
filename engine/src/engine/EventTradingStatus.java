package engine;

import java.util.ArrayList;
import java.util.List;

public class EventTradingStatus {
        private String eventName;
        private boolean isOpen;
        private List<Option> options;
        private float accountBalance; // for the user, not the event
        private float totalCommissionPaid;
        private List<Trade> tradingHistory;

    public EventTradingStatus(String eventName, List<Option> options, float accountBalance) {
        this.eventName = eventName;
        this.isOpen = true;
        this.options = options;
        this.accountBalance = accountBalance;
        this.totalCommissionPaid = 0.0f;
        this.tradingHistory = new ArrayList<Trade>();
    }

    // Getters
    public boolean isOpen() {
        return isOpen;
    }

    public List<Option> getOptions() {
        return options;
    }

    public float getAccountBalance() {
        return accountBalance;
    }

    public float getTotalCommissionPaid() {
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


    public void updateAccountBalance(float newBalance) {
        this.accountBalance = newBalance;
    }


    public void updateTotalCommissionPaid(float newTotal) {
        this.totalCommissionPaid = newTotal;
    }

    public void updateHistory(Trade trade) {
        tradingHistory.add(trade);
    }
}



