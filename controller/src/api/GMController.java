package api;

import dto.*;
import engine.Engine;
import engine.EngineImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.io.IOException;
import java.util.stream.Collectors;

public class GMController {
    private Engine engine = new EngineImpl();

    public void loadFile(String path) throws Exception {
        engine.loadFile(path);
    }

    public List<EventSummaryDTO> getEvents() {
        return engine.getEvents().stream().map(EventSummaryDTO::new).toList();
    }

    public EventTradingStatusDTO getEventTradingStatus(int eventId) throws IllegalArgumentException {
        return new EventTradingStatusDTO(engine.getEventTradingStatus(eventId));
    }

    public PurchaseDTO participateInEvent(String userName, int eventId, int optionNumber, int shares) {
        return new PurchaseDTO(engine.participateInEvent(userName,eventId, optionNumber, shares));
    }

    public void closeEvent(int eventID,int winningOption) {
        engine.closeEvent(eventID, winningOption);
    }


    //bonus: save and load the state of the engine to a file
    public void saveState(String path) throws IOException {
        engine.saveState(path);
    }

    public void loadState(String path) throws IOException, ClassNotFoundException {
        engine.loadState(path);
    }

    public Map<String, UserDTO> getUsers() {
        return engine.getUsers().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> new UserDTO(entry.getValue())
                ));
    }

    public List<UserEventDTO> getUserEvents(String userName) {
        if (!engine.getUsers().containsKey(userName)) {
            throw new IllegalArgumentException("User not found: " + userName);
        }

        UserDTO user = new UserDTO(engine.getUsers().get(userName));
        List<UserEventDTO> userEvents = new ArrayList<>();
        // filter the events the user is participating as trader
        engine.getEvents().stream()
                .filter(event -> event.isParticipating(user.getName()))
                .forEach(event -> {
                userEvents.add(new UserEventDTO(user, event.getEventTradingStatus()));
            });

        // filter the events the user is participating as market maker
        engine.getEvents().stream().filter(event -> user.getEventsIDs().contains(event.getId()))
                .forEach(event -> {
                    // check if the user is already added as trader
                    if (userEvents.stream().noneMatch(ue -> ue.eventName().equals(event.getEventName()))) {
                        userEvents.add(new UserEventDTO(user, event.getEventTradingStatus()));
                    }
                });

        return userEvents;
    }

    public void activateEvent(String name, int eventId) {
        engine.activateEvent(name, eventId);
    }
}
