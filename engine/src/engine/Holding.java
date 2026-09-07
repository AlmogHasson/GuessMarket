package engine;

public class Holding  {
    private int shares;
    private double totalPaid; // net cash spent acquiring current position

    public void applyBuy(int qty, double cost) {
        shares += qty; totalPaid += cost;
    }

    public void applySell(int qty, double proceeds) {
        shares -= qty; totalPaid -= proceeds;
    }

    // getters
    public int getShares() {
        return shares;
    }

    public double getTotalPaid() {
        return totalPaid;
    }
}
