package manager;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ActiveUsersManager {

    private final Set<String> activeUsers =
            ConcurrentHashMap.newKeySet();

    public boolean login(String userName) {
        return activeUsers.add(userName);
    }

    public void logout(String userName) {
        activeUsers.remove(userName);
    }

    public boolean isLoggedIn(String userName) {
        return activeUsers.contains(userName);
    }
}