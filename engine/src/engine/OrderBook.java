package engine;

public class OrderBook implements Method{
    private int d;

    public OrderBook(int d) {
        this.d = d;
    }

    @Override
    public int getValue() {
        return d;
    }

    @Override
    public float calculateOptionValue(int firstOptionShares, int secondOptionShares) {
        return 0;
    }

    @Override
    public float calculateBalance(int totalSharesBought, int totalSharesBought1) {
        return 0;
    }
}
