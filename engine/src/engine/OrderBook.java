package engine;

import generated.GMOrderBook;

public class OrderBook implements Method{
    private int d;
    protected int initial;
    protected String allowMint;

    public OrderBook(GMOrderBook ob) {
        this.d = ob.getD();
        this.initial = ob.getInitial();
        this.allowMint = ob.getAllowMint();
    }

    @Override
    public int getValue() {
        return d;
    }

    public int getInitial() {
        return initial;
    }

    public String getAllowMint() {
        return allowMint;
    }

    public void setInitial(int value) {
        this.initial = value;
    }

    public void setD(int value) {
        this.d = value;
    }

    public void setAllowMint(String value) {
        this.allowMint = value;
    }


    @Override
    public double calculateOptionValue(int firstOptionShares, int secondOptionShares) {
        return 0;
    }

    @Override
    public double calculateBalance(int totalSharesBought, int totalSharesBought1) {
        return 0;
    }
}
