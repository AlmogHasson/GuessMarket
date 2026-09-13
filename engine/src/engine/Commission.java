package engine;

import java.io.Serializable;

public class Commission implements Serializable {

    private final int value;
    private final String commissionType;

    public Commission(generated.Commission commission) {
        this.value = commission.getValue();
        this.commissionType = commission.getType();
    }
    //getters
    public int getValue() {
        return value;
    }
    public String getCommissionType() {
        return commissionType;
    }
}
