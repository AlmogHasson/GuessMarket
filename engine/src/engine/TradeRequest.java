package engine;

/**
 * @param optionNumber 1 or 2
 * @param side         LMSR: always BUY
 * @param price        null for LMSR; required for OB (validated: price <= d - 0.01)
 */
public record TradeRequest(String userName, int optionNumber, int shares, Side side, Double price) {
}
