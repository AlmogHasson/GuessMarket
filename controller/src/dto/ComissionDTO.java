package dto;

import engine.Comission;

public record ComissionDTO(
        int value,
        String type
)
{
    public ComissionDTO (Comission comission) {
        this(comission.getValue(), comission.getType());

    }

}
