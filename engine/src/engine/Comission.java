package engine;

import generated.Comision;

public class Comission {
    private int value;
    private String type;

    public Comission(Comision comision) {
        this.value = comision.getValue();
        this.type = comision.getType();
    }
}
