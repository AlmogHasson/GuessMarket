package dto;

public record ParticipantHoldingDTO(
        String userName,
        String optionName,
        int shares,
        double totalSharesValue,
        double totalPaid
) {
}
