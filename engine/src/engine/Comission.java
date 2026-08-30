package engine;

import generated.Commission;

import java.io.Serializable;

public class Comission implements Serializable {
    private static final long serialVersionUID = 1L;

    private int value;
    private String type;

    public Comission(Commission commission) {
        this.value = commission.getValue();
        this.type = commission.getType();
    }
    //getters
    public int getValue() {
        return value;
    }
    public String getType() {
        return type;
    }
}
