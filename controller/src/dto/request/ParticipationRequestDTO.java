package dto.request;

import dto.Side;

public record ParticipationRequestDTO(
        int eventId,
        int optionNumber,
        int shares,
        Side side,
        Double price
) {
}