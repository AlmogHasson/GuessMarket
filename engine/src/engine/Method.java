package engine;

import java.io.Serializable;

public interface Method extends Serializable {
    int getValue();
    float calculateOptionValue(int firstOptionShares, int secondOptionShares);
    float calculateBalance(int totalSharesBought, int totalSharesBought1);
}
