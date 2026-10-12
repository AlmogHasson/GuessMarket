package dto;

/**
 * A data transfer object for a chat message.
 */
public record ChatMessageDTO(int id, String userName, String text, long timestamp) {}