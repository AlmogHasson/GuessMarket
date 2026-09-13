package engine;
import java.io.File;

import generated.GMEvent;
import generated.GMUser;
import generated.GuessMarket;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Optional;

public class EngineImpl implements Engine {
    List<Event> events;
    Map<String, User> users;


    @Override
    public void loadFile(String path) throws JAXBException {
        // validate the file path : exists, readable, not null or empty
        validateFilePath(path);

        JAXBContext jaxbContext = JAXBContext.newInstance(GuessMarket.class);
        GuessMarket guessMarket = (GuessMarket) jaxbContext.createUnmarshaller().unmarshal(new File(path));

        validateIds(guessMarket);
        // check if commission is between 0 and 90
        validateCommissions(guessMarket);

        validateUsers(guessMarket);

        validateMarketMakers(guessMarket);

        //when the file is valid, load the events and users
        loadEvents(guessMarket);
        loadUsers(guessMarket);

    }

    @Override
    public Map<String, User> getUsers() {
        return users;
    }

    private void validateUsers(GuessMarket guessMarket) {
        java.util.Set<String> names = new java.util.HashSet<>();

        for (var user : guessMarket.getGMUsers().getGMUser()) {
            if (!names.add(user.getName())) {
                throw new IllegalArgumentException(
                        "Duplicate user name: \"" + user.getName() + "\"."
                );
            }

            if (user.getInitialCash() <= 0) {
                throw new IllegalArgumentException(
                        "User \"" + user.getName()
                                + "\" has invalid initial cash: "
                                + user.getInitialCash()
                                + ". Initial cash must be greater than 0."
                );
            }
        }
    }

    private void loadUsers(GuessMarket guessMarket) {
        users = new java.util.HashMap<>();
        guessMarket.getGMUsers().getGMUser().forEach(gmUser -> {
            User user = new User();
            user.setName(gmUser.getName());
            user.setAccountBalance(gmUser.getInitialCash());
            user.setUserEvents(
                    Optional.ofNullable(gmUser.getGMMarketMaker())
                            .map(mm -> mm.getEvent().stream()
                                    .map(generated.Event::getId)
                                    .toList())
                            .orElse(List.of()));
            users.put(user.getName(), user);
        });
    }

    private void loadEvents(GuessMarket guessMarket) {
        events.clear();
        guessMarket.getGMEvents().getGMEvent().forEach(gmEvent -> {
            events.add(new Event(gmEvent));
        });
    }

    @Override
    public List<Event> getEvents() {
        return events;
    }

    @Override
    public EventTradingStatus getEventTradingStatus(int eventId) {
        Event event = events.stream().filter(e -> e.getId() == eventId).findFirst().orElse(null);
        if (event == null) {
            throw new IllegalArgumentException("Event with ID " + eventId + " not found");
        }
        return event.getEventTradingStatus();
    }


    @Override
    public Purchase participateInEvent(String userName, int eventId, int optionNumber, int shares, Side side, Double price) {
        Event event = events.stream().filter(e -> e.getId() == eventId).findFirst().orElse(null);
        User user = users.get(userName);

        validateParticipationParams(userName, eventId, optionNumber, shares, user, event);

        if (event.getMethod() instanceof OrderBook) {
            if (side == null) {
                throw new IllegalArgumentException("Side (BUY/SELL) is required for order book trading");
            }
            if (price == null) {
                throw new IllegalArgumentException("Price is required for order book trading");
            }

            validateSharesQuantity(userName, optionNumber, shares, side, event);

        } else {
            // LMSR: always a buy against the curve; price has no meaning here
            side = Side.BUY;
            price = null;
        }

        TradeResult result = event.participate(user, users, optionNumber, shares, side, price);
        return new Purchase(result.netCostToInitiator(),
                result.netCostToInitiator() - result.commissionPaid(),
                result.commissionPaid());
    }

    private void validateMarketMakers(GuessMarket guessMarket) {
        java.util.Set<Integer> eventIds = new java.util.HashSet<>();
        java.util.Map<Integer, Integer> makerCounts = new java.util.HashMap<>();

        for (var event : guessMarket.getGMEvents().getGMEvent()) {
            eventIds.add(event.getId());
        }

        for (var user : guessMarket.getGMUsers().getGMUser()) {
            var assignments = user.getGMMarketMaker();

            if (assignments == null) {
                continue;
            }

            // Count each user only once per event.
            java.util.Set<Integer> assignedIds = new java.util.HashSet<>();

            for (var assignment : assignments.getEvent()) {
                int eventId = assignment.getId();

                if (!eventIds.contains(eventId)) {
                    throw new IllegalArgumentException(
                            "User \"" + user.getName()
                                    + "\" is assigned as market maker for event "
                                    + eventId + ", but that event does not exist."
                    );
                }

                if (assignedIds.add(eventId)) {
                    makerCounts.merge(eventId, 1, Integer::sum);
                }
            }
        }

        for (var event : guessMarket.getGMEvents().getGMEvent()) {
            int count = makerCounts.getOrDefault(event.getId(), 0);

            if (count != 1) {
                throw new IllegalArgumentException(
                        "Event " + event.getId()
                                + " must have exactly one market maker, but has "
                                + count + "."
                );
            }
        }
    }

    private static void validateSharesQuantity(
            String userName, int optionNumber, int shares,
            Side side, Event event)
    {
        OrderBook orderBook = (OrderBook) event.getMethod();

        if (side == Side.SELL) {
            int ownedShares =
                    event.getOrCreateHolding(userName, optionNumber).getShares();

            int reservedShares =
                    orderBook.getReservedSellShares(userName, optionNumber);

            int availableShares = ownedShares - reservedShares;

            if (shares > availableShares) {
                throw new IllegalArgumentException(
                        "Not enough available shares to sell. User "
                                + userName
                                + " owns " + ownedShares
                                + " shares, has " + reservedShares
                                + " shares already offered for sale, and only "
                                + availableShares + " shares are available.");
            }
        }
    }

    private static void validateParticipationParams(String userName, int eventId, int optionNumber, int shares, User user, Event event) {
        if (user == null) {
            throw new IllegalArgumentException("User with name " + userName + " not found");
        }

        if (event == null) {
            throw new IllegalArgumentException("Event with ID " + eventId + " not found");
        }

        if (!event.isOpen()){
            throw new IllegalStateException("Event with ID " + eventId + " is closed for trading");
        }

        if (optionNumber < 1 || optionNumber > event.getOptions().size()) {
            throw new IllegalArgumentException("Invalid option number: " + optionNumber);
        }
        
        if (shares <= 0) {
            throw new IllegalArgumentException("Shares must be greater than 0");
        }

        if (user.getAccountBalance() < 0) {
            throw new IllegalArgumentException("User with name " + user.name + " has insufficient funds");
        }
    }

    @Override
    public void closeEvent(int eventId,int winningOption)throws  IllegalArgumentException{
        Event event = events.stream().filter(e -> e.getId() == eventId).findFirst().orElse(null);
        if (event == null) {
            throw new IllegalArgumentException("Event with ID " + eventId + " not found");
        }
        event.getMethod().close(event, users, winningOption);

    }

    @Override
    public void activateEvent(String name, int eventId) {
        Event event = events.stream().filter(e -> e.getId() == eventId).findFirst().orElse(null);
        if (event == null) {
            throw new IllegalArgumentException("Event with ID " + eventId + " not found");
        }
        User user = users.get(name);
        if (user == null) {
            throw new IllegalArgumentException("User with name " + name + " not found");
        }
        if (!user.isMarketMaker(eventId)) {
            throw new IllegalArgumentException("User with name " + name + " is not the market maker for event ID " + eventId);
        }
        event.activate(user);
    }

    @Override
    public List<String> getEventParticipants(int eventId) {
        Event event = events.stream().filter(e -> e.getId() == eventId).findFirst().orElse(null);
        if (event == null) {
            throw new IllegalArgumentException("Event with ID " + eventId + " not found");
        }
        return new ArrayList<>(event.getParticipantNames());
    }

    @Override
    public int getParticipantShares(int eventId, String userName, int optionNumber) {
        Event event = events.stream().filter(e -> e.getId() == eventId).findFirst().orElse(null);
        if (event == null) {
            throw new IllegalArgumentException("Event with ID " + eventId + " not found");
        }
        Holding holding = event.getUsersHoldings().get(userName) == null
                ? null
                : event.getUsersHoldings().get(userName)[optionNumber - 1];
        return holding == null ? 0 : holding.getShares();
    }


    private void validateCommissions(GuessMarket guessMarket) {
        var invalidIds = guessMarket.getGMEvents().getGMEvent().stream()
                .filter(e -> {
                    int commission = e.getCommission().getValue();
                    return commission < 0 || commission > 90;
                })
                .map(GMEvent::getId)
                .toList();

        if (!invalidIds.isEmpty()) {
            throw new IllegalArgumentException("Invalid commission (must be 0-90) for event IDs: " + invalidIds);
        }
    }

    private void validateIds(GuessMarket guessMarket) {
        if (!hasUniqueIds(guessMarket.getGMEvents().getGMEvent())) {
            throw new IllegalArgumentException("Events must have unique IDs");
        }
    }

    private static void validateFilePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        File file = new File(path);
        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist");
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException("Path is not a file: " + file.getAbsolutePath());
        }
        // check if the file is readable
        if (!file.canRead()) {
            throw new IllegalArgumentException("File is not readable");
        }
    }


    public EngineImpl() {
        this.events = new ArrayList<>();
    }


    private boolean hasUniqueIds(List<GMEvent> gmEvent) {
        return gmEvent.stream().map(GMEvent::getId).distinct().count() == gmEvent.size();
    }

    @Override
    public void saveState(String path) throws IOException {
        try (ObjectOutputStream out =
                     new ObjectOutputStream(
                             new FileOutputStream(path + ".gm"))) {

            out.writeObject(events);
        }
    }


    @Override
    @SuppressWarnings("unchecked")
    public void loadState(String path) throws IOException, ClassNotFoundException {
        List<Event> loadedEvents;

        try (ObjectInputStream in =
                     new ObjectInputStream(
                             new FileInputStream(path + ".gm"))) {

            loadedEvents = (List<Event>) in.readObject();
        }

        events = loadedEvents;
    }
}
