package dto;

public record MethodDTO (
        LMSRDTO lmsr
) {
    public MethodDTO(LMSRDTO lmsr) {
        this.lmsr = lmsr;
    }
}

