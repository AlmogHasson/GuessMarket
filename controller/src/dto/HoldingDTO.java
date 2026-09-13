package dto;

public record HoldingDTO(
        String optionName,
        int shares,
        double totalPaid,
        double commissionPaid
) { }
