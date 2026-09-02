package engine;

import java.io.Serializable;

public class LMSR implements Method,Serializable {
    private int b;

    public LMSR(int b) {
        this.b = b;
    }

    @Override
    public int getValue() {
        return b;
    }

    /**
     * Current value of the FIRST option, i.e. softmax(q1/b, q2/b).
     * The exponents are shifted by their maximum before exponentiating
     * (log-sum-exp trick). Mathematically identical to exp(a)/(exp(a)+exp(c)),
     * because the common factor exp(-m) cancels top and bottom, but the largest
     * term is now exp(0)=1 so nothing can overflow. The smaller term may
     * underflow to 0.0, which is the correct limit rather than an error.
     */
    @Override
    public double calculateOptionValue(int firstOptionShares, int secondOptionShares) {
        double a = (double) firstOptionShares / b;
        double c = (double) secondOptionShares / b;
        double m = Math.max(a, c);

        double optionExp = Math.exp(a - m);
        double otherExp = Math.exp(c - m);

        return (optionExp / (optionExp + otherExp));
    }

    //amount in the event pool
    /**
     * Amount in the event pool: b * ln(exp(q1/b) + exp(q2/b)).
     * Rewritten as b * (m + ln(exp(a-m) + exp(c-m))) with m = max(a, c).
     * Same identity, but the argument to exp never exceeds 0, so the
     * ln(Infinity) that produced Infinity in the Paid column cannot occur.
     */
    @Override
    public double calculateBalance(int firstOptionShares, int secondOptionShares) {
        double a = (double) firstOptionShares / b;
        double c = (double) secondOptionShares / b;
        double m = Math.max(a, c);

        return (b * (m + Math.log(Math.exp(a - m) + Math.exp(c - m))));
    }
}
