package engine;

import generated.GMLMSR;
import generated.GMMethod;
import java.io.Serializable;

public class Method implements Serializable {
    private static final long serialVersionUID = 1L;

    private LMSR lmsr;

    public Method(GMMethod method) {
        this.lmsr = new LMSR(method.getGMLMSR().getB());
    }

    //getters
    public LMSR getLmsr() {
        return lmsr;
    }
}
