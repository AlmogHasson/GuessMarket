package dto.request;

public record CloseEventRequestDTO(
        int eventId,
        int winningOption
) {
}