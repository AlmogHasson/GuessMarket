package engine;

public class LMSR {
    private int b;

    public LMSR(int b) {
        this.b = b;
    }

    public int getB() {
        return b;
    }

    // Current value of one option
    public double calculateOptionValue(int optionShares, int otherOptionShares) {
        double optionExp = Math.exp((double) optionShares / b);
        double otherExp = Math.exp((double) otherOptionShares / b);

        return optionExp / (optionExp + otherExp);
    }

    // Current LMSR cost / amount in the event pool
    public double calculateCost(int firstOptionShares, int secondOptionShares) {
        return b * Math.log(
                Math.exp((double) firstOptionShares / b) + Math.exp((double) secondOptionShares / b)
        );
    }
}
