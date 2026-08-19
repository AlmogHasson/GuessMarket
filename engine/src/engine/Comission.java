package engine;

import generated.Comision;
import java.io.Serializable;

public class Comission implements Serializable {
    private static final long serialVersionUID = 1L;

    private int value;
    private String type;

    public Comission(Comision comision) {
        this.value = comision.getValue();
        this.type = comision.getType();
    }
    //getters
    public int getValue() {
        return value;
    }
    public String getType() {
        return type;
    }
}
