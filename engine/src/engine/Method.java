package engine;

import java.io.Serializable;

public interface Method extends Serializable {
    int getValue();
    double calculateOptionValue(int firstOptionShares, int secondOptionShares);
    double calculateBalance(int totalSharesBought, int totalSharesBought1);
}
