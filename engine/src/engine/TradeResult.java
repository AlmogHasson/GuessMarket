package engine;

import java.util.List;

public class TradeResult {
    private final double netCostToInitiator; // + = paid, - = received (OB sell)
    private final double commissionPaid;
    private final List<Trade> trades;        // every fill produced, both sides of each match
    private final int remainingResting;      // 0 for LMSR; >0 if part of an OB order still rests

    public TradeResult(double netCostToInitiator, double commissionPaid, List<Trade> trades, int remainingResting) {
        this.netCostToInitiator = netCostToInitiator;
        this.commissionPaid = commissionPaid;
        this.trades = trades;
        this.remainingResting = remainingResting;
    }

    public double getNetCostToInitiator() {
        return netCostToInitiator;
    }

    public double getCommissionPaid() {
        return commissionPaid;
    }

    public List<Trade> getTrades() {
        return trades;
    }

    public int getRemainingResting() {
        return remainingResting;
    }
}
