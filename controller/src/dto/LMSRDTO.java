package dto;

import engine.LMSR;

public record LMSRDTO(
        int b
) {
    // Constructor to create dto.LMSRDTO from LMSR
    public LMSRDTO(LMSR lmsr) {
        this(lmsr.getB());
    }

    //getters
    public int getB() {
        return b;
    }
}
