package engine;

import generated.GMLMSR;
import generated.GMMethod;

public class Method {
    private LMSR lmsr;

    public Method(GMMethod method) {
        this.lmsr = new LMSR(method.getGMLMSR().getB());
    }

    //getters
    public LMSR getLmsr() {
        return lmsr;
    }
}
