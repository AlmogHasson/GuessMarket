package dto;

import engine.Comission;

public record CommissionDTO(
        int value,
        String type
)
{
    public CommissionDTO(Comission comission) {
        this(comission.getValue(), comission.getType());

    }

}
