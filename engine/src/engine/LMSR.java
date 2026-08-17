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
    ///@return returns the first param's value
    public float calculateOptionValue(int firstOptionShares, int secondOptionShares) {
        float optionExp = (float) Math.exp((double) firstOptionShares / b);
        float otherExp = (float) Math.exp((double) secondOptionShares / b);

        return (float) (optionExp / (optionExp + otherExp));
    }

    // amount in the event pool
    public float calculateBalance(int firstOptionShares, int secondOptionShares) {
        return b * (float) Math.log(
                Math.exp((double) firstOptionShares / b) + Math.exp((double) secondOptionShares / b)
        );
    }
}
