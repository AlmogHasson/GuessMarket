package engine;

import java.io.Serial;
import java.io.Serializable;

public class LMSR implements Method,Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private int b;

    public LMSR(int b) {
        this.b = b;
    }

    @Override
    public int getValue() {
        return b;
    }

    ///@return returns the first param's value
    @Override
    public float calculateOptionValue(int firstOptionShares, int secondOptionShares) {
        float optionExp = (float) Math.exp((double) firstOptionShares / b);
        float otherExp = (float) Math.exp((double) secondOptionShares / b);

        return (float) (optionExp / (optionExp + otherExp));
    }

    //amount in the event pool
    @Override
    public float calculateBalance(int firstOptionShares, int secondOptionShares) {
        return b * (float) Math.log(
                Math.exp((double) firstOptionShares / b) + Math.exp((double) secondOptionShares / b)
        );
    }
}
