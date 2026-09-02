package engine;

public class Purchase {
    private double totalPaid;
    private double sharePaid;
    private double commissionPaid;

    public Purchase(double totalPaid, double sharePaid, double commissionPaid) {
        this.totalPaid = totalPaid;
        this.sharePaid = sharePaid;
        this.commissionPaid = commissionPaid;
    }


    public double
    getTotalPaid() { return totalPaid; }

    public double
    getSharePaid() {
        return sharePaid;
    }


    public double getCommissionPaid() {
        return commissionPaid;
    }
}
