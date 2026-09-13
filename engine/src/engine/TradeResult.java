package engine;

import java.util.List;

/**
 * @param netCostToInitiator + = paid, - = received (OB sell)
 * @param trades             every fill produced, both sides of each match
 * @param remainingResting   0 for LMSR; >0 if part of an OB order still rests
 */
public record TradeResult(double netCostToInitiator, double commissionPaid, List<Trade> trades, int remainingResting) {
}
