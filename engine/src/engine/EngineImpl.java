package engine;
import java.io.File;
import generated.GMEvent;
import generated.GuessMarket;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EngineImpl implements Engine {
    private final List<Event> events = new ArrayList<>();
    Map<String, User> users =  new ConcurrentHashMap<>();
    EventIdGenerator eventIdGenerator = new EventIdGenerator();


    @Override
    public void loadFile(String path) throws JAXBException {
        // validate the file path : exists, readable, not null or empty
        validateFilePath(path);

        JAXBContext jaxbContext = JAXBContext.newInstance(GuessMarket.class);
        GuessMarket guessMarket = (GuessMarket) jaxbContext.createUnmarshaller().unmarshal(new File(path));

        validateEventNames(guessMarket);
        validateCommissions(guessMarket); // check if commission is between 0 and 90

        //when the file is valid, load the events
        loadEvents(guessMarket);
    }

    private void validateEventNames(GuessMarket guessMarket) {
        List<String> eventNames = guessMarket.getGMEvents().getGMEvent()
                .stream()
                .map(GMEvent::getName)
                .toList();

        List<String> duplicateEventNames = eventNames.stream()
                .filter(name -> eventNames.stream().filter(n -> n.equals(name)).count() > 1)
                .toList();

        if (!duplicateEventNames.isEmpty()) {
            throw new IllegalArgumentException("Duplicate event names found: " + duplicateEventNames);
        }

    }

    @Override
    public User getOrCreateUser(String userName) {
        return users.computeIfAbsent(userName, name -> {
            User user = new User();
            user.setName(name);
            user.setAccountBalance(0);
            return user;
        });
    }

    @Override
    public Map<String, User> getUsers() {
        return users;
    }

    private void loadEvents(GuessMarket guessMarket) {
        guessMarket.getGMEvents().getGMEvent().forEach(gmEvent -> {
            events.add(new Event(gmEvent,eventIdGenerator.getIdAndIncrement()));
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
        var invalidNames = guessMarket.getGMEvents().getGMEvent()
                .stream()
                .filter(e -> {
                    int commission = e.getCommission().getValue();
                    return commission < 0 || commission > 90;
                })
                .map(GMEvent::getName)
                .toList();

        if (!invalidNames.isEmpty()) {
            throw new IllegalArgumentException("Invalid commission (must be 0-90) for event names: " + invalidNames);
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
}