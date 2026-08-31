package dto;

import engine.Comission;

public record CommissionDTO(
        int value,
        String commissionType
)
{
    public CommissionDTO(Comission comission) {
        this(comission.getValue(), comission.getCommissionType());

    }
}
