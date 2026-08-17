package engine;

import java.util.Arrays;
import java.util.List;

public class EventTradingStatus {
        private boolean isOpen;
        private List<OptionTradingStatus> options;
        private float accountBalance; // for the user, not the event
        private float totalCommissionPaid;
        private List<Trade> tradingHistory;

    public EventTradingStatus(List<Option> options) {
        this.isOpen = true;
        this.options = options.stream().map(OptionTradingStatus::new).toList();
        this.accountBalance = 0.0f;
        this.totalCommissionPaid = 0.0f;
        this.tradingHistory = List.of();
    }

    // Getters
    public boolean isOpen() {
        return isOpen;
    }

    public List<OptionTradingStatus> getOptions() {
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

    //getOptionTradingStatuses() method to return the list of OptionTradingStatus objects
    public List<OptionTradingStatus> getOptionTradingStatuses() {
        return options;
    }
}



