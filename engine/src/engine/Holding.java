package engine;

public class Holding  {
    String optionName;
    private int shares;
    private double totalPaid;
    private double commissionPaid;
    // net cash spent acquiring current position

    public Holding(String optionName) {
        this.optionName = optionName;
        this.shares = 0;
        this.totalPaid = 0.0;
        this.commissionPaid = 0.0;
    }

    public void applyBuy(int qty, double cost) {
        shares += qty; totalPaid += cost;
    }

    public void applySell(int qty, double proceeds) {
        if (shares < qty) {
            throw new IllegalArgumentException("Not enough shares to sell");
        }
        shares -= qty; totalPaid -= proceeds;
    }

    public void addCommissionPaid(double commission) {
        commissionPaid += commission;
    }

    // getters
    public int getShares() {
        return shares;
    }

    public double getTotalPaid() {
        return totalPaid;
    }

    public double getCommissionPaid() {
        return commissionPaid;
    }

}
