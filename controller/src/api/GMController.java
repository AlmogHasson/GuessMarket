package api;

import dto.*;
import dto.Side;
import engine.*;

import java.util.*;
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
        return new EventTradingStatusDTO(engine.getEvents().stream().filter(e -> e.getId() == eventId).findFirst().orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId)));
    }

    public PurchaseDTO participateInEvent(String userName, int eventId, int optionNumber, int shares, Side side, Double price) {
        return new PurchaseDTO(engine.participateInEvent(userName, eventId, optionNumber, shares, side.toEngine(), price));
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
        Map<String, User> users = engine.getUsers();

        if (users == null) {
            return Collections.emptyMap();
        }

        return users.entrySet().stream()
                .filter(Objects::nonNull)
                .filter(entry -> entry.getKey() != null)
                .filter(entry -> entry.getValue() != null)
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
                userEvents.add(new UserEventDTO(user, event));
            });

        // filter the events the user is participating as market maker
        engine.getEvents().stream().filter(event -> user.getEventsIDs().contains(event.getId()))
                .forEach(event -> {
                    // check if the user is already added as trader
                    if (userEvents.stream().noneMatch(ue -> ue.eventName().equals(event.getEventName()))) {
                        userEvents.add(new UserEventDTO(user, event));
                    }
                });

        return userEvents;
    }

    public void activateEvent(String name, int eventId) {
        engine.activateEvent(name, eventId);
    }

    public List<ParticipantHoldingDTO> getEventParticipants(int eventId) {
        List<OptionDTO> options = getEventTradingStatus(eventId).optionTradingStatus();
        List<ParticipantHoldingDTO> rows = new ArrayList<>();
            for (int i = 0; i < options.size(); i++) {
        for (String userName : engine.getEventParticipants(eventId)) {
                int optionNumber = i + 1;
                OptionDTO option = options.get(i);
                int shares = engine.getParticipantShares(eventId, userName, optionNumber);
                double value = shares * option.currentValue();
                Holding[] userHoldings = engine.getEvents().get(eventId - 1).getUserHoldings(userName);
                Holding userHolding = userHoldings[i];
                double totalPaid = userHolding.getTotalPaid();
                rows.add(new ParticipantHoldingDTO(userName, option.optionName(), shares, value, totalPaid));
        }
            }
        return rows;
    }

    public UserOrderBookPositionDTO getUserOrderBookPosition(String userName, int eventId) {
        Event event = engine.getEvents().stream()
                .filter(e -> e.getId() == eventId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        User user = engine.getUsers().get(userName);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + userName);
        }
        return new UserOrderBookPositionDTO(event, user);
    }
}
