package dto;

import engine.Holding;

public record HoldingDTO(
        String optionName,
        int shares,
        double totalPaid,
        double commissionPaid
) {

}
