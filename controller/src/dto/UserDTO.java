package dto;

import engine.User;
import java.util.List;

public record UserDTO(
        double accountBalance, //initialCash in the generated files
        List<Integer> eventsIDs,
        String name

) {
    public UserDTO(User user){
        this(
                user.getAccountBalance(),
                user.getUserEvents(),
                user.getName()
        );
    }

    //getters
    public double getAccountBalance() {
        return accountBalance;
}

    public List<Integer> getEventsIDs() {
        return eventsIDs;
    }

    public String getName() {
        return name;
    }

    public boolean isEventMaker(int eventID) {
        return eventsIDs.contains(eventID);
    }

    public boolean isBlocked() {
        return accountBalance < 0;
    }
}
