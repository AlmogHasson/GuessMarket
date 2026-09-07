package engine;

public class TradeRequest {
    private final String userName;
    private final int optionNumber;   // 1 or 2
    private final int shares;
    private final Side side;          // LMSR: always BUY
    private final Double price;       // null for LMSR; required for OB (validated: price <= d - 0.01)

    public TradeRequest(String userName, int optionNumber, int shares, Side side, Double price) {
        this.userName = userName;
        this.optionNumber = optionNumber;
        this.shares = shares;
        this.side = side;
        this.price = price;
    }

    public String getUserName() {
        return userName;
    }

    public int getOptionNumber() {
        return optionNumber;
    }

    public int getShares() {
        return shares;
    }

    public Side getSide() {
        return side;
    }

    public Double getPrice() {
        return price;
    }
}
