package dto;

import engine.LMSR;

public record LMSRDTO(
        int b
) implements MethodDTO {

    // Constructor to create LMSRDTO from LMSR
    public LMSRDTO(LMSR lmsr) {
        this(lmsr.getValue());
    }

    @Override
    public int getValue() {
        return b;
    }

    @Override
    public String getName() { return "lmsr";}
}