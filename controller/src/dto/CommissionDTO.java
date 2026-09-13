package dto;

import engine.Commission;

public record CommissionDTO(
        int value,
        String commissionType
)
{
    public CommissionDTO(Commission comission) {
        this(comission.getValue(), comission.getCommissionType());

    }
}
