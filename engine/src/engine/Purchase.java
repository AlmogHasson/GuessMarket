package engine;

public class Purchase {
    private float totalPaid;
    private float sharePaid;
    private float commissionPaid;

    public Purchase(float totalPaid, float sharePaid, float commissionPaid) {
        this.totalPaid = totalPaid;
        this.sharePaid = sharePaid;
        this.commissionPaid = commissionPaid;
    }


    public float
    getTotalPaid() { return totalPaid; }

    public float
    getSharePaid() {
        return sharePaid;
    }


    public float getCommissionPaid() {
        return commissionPaid;
    }
}
