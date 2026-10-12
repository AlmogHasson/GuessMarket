package api;

import dto.*;
import dto.Side;
import engine.*;
import java.util.*;
import java.util.stream.Collectors;

public class GMController {
    private Engine engine = new EngineImpl();

    public synchronized void loadFile(java.io.InputStream inputStream, String uploaderName) throws Exception {

        int oldCount = engine.getEvents().size();

        try {
            engine.loadFile(inputStream);
        } catch (Exception e) {
            List<Event> all = engine.getEvents();
            all.subList(oldCount, all.size()).clear();
            throw e;
        }

        User uploader = engine.getOrCreateUser(uploaderName);
        List<Event> events = engine.getEvents();

        for (int i = oldCount; i < events.size(); i++) {
            uploader.addUserEvent(events.get(i).getId());
        }
    }

    public synchronized UserDTO getOrCreateUser(String userName) {
        return new UserDTO(engine.getOrCreateUser(userName));
    }

    public synchronized List<EventSummaryDTO> getEvents() {
        return engine.getEvents().stream().map(EventSummaryDTO::new).toList();
    }

    public synchronized EventTradingStatusDTO getEventTradingStatus(int eventId) throws IllegalArgumentException {
        return new EventTradingStatusDTO(engine.getEvents().stream().filter(e -> e.getId() == eventId).findFirst().orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId)));
    }

    public synchronized PurchaseDTO participateInEvent(String userName, int eventId, int optionNumber, int shares, Side side, Double price) {
        return new PurchaseDTO(engine.participateInEvent(userName, eventId, optionNumber, shares, side.toEngine(), price));
    }

    public synchronized void closeEvent(String userName, int eventId, int winningOption) {
        engine.closeEvent(userName, eventId, winningOption);
    }

    public synchronized Map<String, UserDTO> getUsers() {
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

    public synchronized List<UserEventDTO> getUserEvents(String userName) {
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

    public synchronized void activateEvent(String name, int eventId) {
        engine.activateEvent(name, eventId);
    }

    public synchronized List<ParticipantHoldingDTO> getEventParticipants(int eventId) {

        Event event = engine.getEvents()
                .stream()
                .filter(e -> e.getId() == eventId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        List<OptionDTO> options = getEventTradingStatus(eventId).optionTradingStatus();

        List<ParticipantHoldingDTO> rows = new ArrayList<>();

        for (int i = 0; i < options.size(); i++)
        {
            for (String userName : engine.getEventParticipants(eventId)) {
                int optionNumber = i + 1;

                OptionDTO option = options.get(i);

                int shares = engine.getParticipantShares(eventId, userName, optionNumber);

                double value = shares * option.currentValue();

                Holding[] userHoldings = event.getUserHoldings(userName);

                double totalPaid = (userHoldings == null || userHoldings[i] == null)
                        ? 0.0
                        : userHoldings[i].getTotalPaid();

                rows.add(new ParticipantHoldingDTO(userName, option.optionName(), shares, value, totalPaid));
            }
        }
        return rows;
    }

    public synchronized UserOrderBookPositionDTO getUserOrderBookPosition(String userName, int eventId) {
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



    public synchronized List<OrderBookStatsDTO> getOrderBookStats(int eventId) {
        Event event = engine.getEvents()
                .stream()
                .filter(e -> e.getId() == eventId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        if (!(event.getMethod() instanceof engine.OrderBook ob)) {
            return List.of(); // LMSR events have no order book stats
        }
        return event.getEventTradingStatus().getOptions()
                .stream()
                .map(opt -> new OrderBookStatsDTO(ob, opt.getOptionNumber()))
                .toList();
    }

    public synchronized UserDTO deposit(String userName, double amount) {
        return new UserDTO(engine.deposit(userName, amount));
    }

    public synchronized List<AccountTransactionDTO> getAccountTransactions(
            String userName
    ) {
        User user = engine.getUsers().get(userName);

        if (user == null) {
            throw new IllegalArgumentException(
                    "User not found: " + userName
            );
        }

        return user.getAccountTransactions()
                .stream()
                .map(AccountTransactionDTO::new)
                .toList();
    }
}