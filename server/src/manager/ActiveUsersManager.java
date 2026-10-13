package manager;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ActiveUsersManager {

    private final Set<String> activeUsers = ConcurrentHashMap.newKeySet();

    private String normalize(String userName) {
        return userName.trim().toLowerCase(Locale.ROOT);
    }

    public boolean login(String userName) {
        return activeUsers.add(normalize(userName));
    }

    public void logout(String userName) {
        activeUsers.remove(normalize(userName));
    }

    public boolean isLoggedIn(String userName) {
        return activeUsers.contains(normalize(userName));
    }
}