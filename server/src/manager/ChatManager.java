package manager;

import dto.ChatMessageDTO;
import java.util.ArrayList;
import java.util.List;

/**
 * A manager for handling chat messages.
 */
public class ChatManager {

    private static final int MAX_MESSAGES = 200;
    private static final int MAX_LENGTH   = 500;

    private final List<ChatMessageDTO> messages = new ArrayList<>();
    private int nextId = 1;

    public synchronized void add(String userName, String text) {
        String clean = text == null ? "" : text.trim();
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("Message cannot be empty.");
        }
        if (clean.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Message is too long (max " + MAX_LENGTH + " characters).");
        }
        messages.add(new ChatMessageDTO(nextId++, userName, clean, System.currentTimeMillis()));
        if (messages.size() > MAX_MESSAGES) {
            messages.remove(0);
        }
    }

    public synchronized List<ChatMessageDTO> getSince(int lastId) {
        return messages.stream().filter(m -> m.id() > lastId).toList();
    }
}