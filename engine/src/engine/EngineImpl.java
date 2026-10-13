package engine;
import java.io.File;
import generated.GMEvent;
import generated.GuessMarket;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class EngineImpl implements Engine {
    private final List<Event> events = new ArrayList<>();
    Map<String, User> users =  new ConcurrentHashMap<>();
    EventIdGenerator eventIdGenerator = new EventIdGenerator();



    @Override
    public void loadFile(java.io.InputStream inputStream)
            throws JAXBException {

        if (inputStream == null) {
            throw new IllegalArgumentException(
                    "No XML input was provided.");
        }

        org.w3c.dom.Document document;

        try {
            javax.xml.parsers.DocumentBuilderFactory factory =
                    javax.xml.parsers.DocumentBuilderFactory.newInstance();

            factory.setFeature(
                    javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING,
                    true);

            factory.setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl",
                    true);

            factory.setFeature(
                    "http://xml.org/sax/features/external-general-entities",
                    false);

            factory.setFeature(
                    "http://xml.org/sax/features/external-parameter-entities",
                    false);

            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setNamespaceAware(true);

            javax.xml.parsers.DocumentBuilder builder =
                    factory.newDocumentBuilder();

            builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler() {
                @Override
                public void fatalError(
                        org.xml.sax.SAXParseException e)
                        throws org.xml.sax.SAXException {
                    throw e;
                }

                @Override
                public void error(
                        org.xml.sax.SAXParseException e)
                        throws org.xml.sax.SAXException {
                    throw e;
                }
            });

            document = builder.parse(inputStream);

        } catch (org.xml.sax.SAXParseException e) {

            if (e.getMessage() != null
                    && e.getMessage().contains(
                    "Content is not allowed in prolog")) {
                throw new IllegalArgumentException(
                        "The selected file does not contain valid XML.");
            }

            throw new IllegalArgumentException(
                    "Malformed XML near line " + e.getLineNumber()
                            + ", column " + e.getColumnNumber()
                            + ". Check that all XML tags are "
                            + "correctly closed and nested.");

        } catch (org.xml.sax.SAXException e) {
            throw new IllegalArgumentException(
                    "Invalid XML document: " + e.getMessage());

        } catch (javax.xml.parsers.ParserConfigurationException
                 | java.io.IOException e) {
            throw new IllegalArgumentException(
                    "Could not read XML document: " + e.getMessage(), e);
        }

        String rootName =
                document.getDocumentElement().getTagName();

        if (!"Guess-Market".equals(rootName)) {
            throw new IllegalArgumentException(
                    "Invalid XML root element: " + rootName
                            + ". Expected Guess-Market.");
        }

        validateRawCommissions(document);

        JAXBContext context =
                JAXBContext.newInstance(GuessMarket.class);

        GuessMarket guessMarket =
                (GuessMarket) context
                        .createUnmarshaller()
                        .unmarshal(document);

        validateEventStructure(guessMarket);
        validateEventNames(guessMarket);
        validateCommissions(guessMarket);

        loadEvents(guessMarket);
    }


    private void validateRawCommissions(
            org.w3c.dom.Document document) {

        org.w3c.dom.NodeList events =
                document.getElementsByTagName("GM-event");

        for (int i = 0; i < events.getLength(); i++) {

            org.w3c.dom.Element event =
                    (org.w3c.dom.Element) events.item(i);

            String eventName = event.getAttribute("name");

            org.w3c.dom.NodeList commissions =
                    event.getElementsByTagName("commission");

            if (commissions.getLength() == 0) {
                throw new IllegalArgumentException(
                        "Event '" + eventName
                                + "' is missing its commission.");
            }

            String rawValue =
                    commissions.item(0).getTextContent().trim();

            int commission;

            try {
                commission = Integer.parseInt(rawValue);

            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Invalid commission for event '"
                                + eventName + "': '"
                                + rawValue
                                + "' is not a valid integer.");
            }

            if (commission < 0 || commission > 90) {
                throw new IllegalArgumentException(
                        "Invalid commission for event '"
                                + eventName
                                + "': value must be between 0 and 90.");
            }
        }
    }



    private void validateEventStructure(GuessMarket guessMarket) {

        if (guessMarket == null || guessMarket.getGMEvents() == null) {
            throw new IllegalArgumentException("XML file must contain GM-events.");
        }

        List<GMEvent> gmEvents = guessMarket.getGMEvents().getGMEvent();

        if (gmEvents == null || gmEvents.isEmpty()) {
            throw new IllegalArgumentException("XML file must contain at least one GM-event.");
        }

        for (int i = 0; i < gmEvents.size(); i++) {
            GMEvent gmEvent = gmEvents.get(i);

            if (gmEvent == null) {
                throw new IllegalArgumentException("Event #" + (i + 1) + " is invalid.");
            }

            String name = gmEvent.getName();

            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Event #" + (i + 1) + " has no valid name.");
            }

            if (gmEvent.getDescription() == null) {
                throw new IllegalArgumentException("Event '" + name + "' is missing its description.");
            }

            if (gmEvent.getCommission() == null) {
                throw new IllegalArgumentException("Event '" + name + "' is missing its commission.");
            }

            String commissionType = gmEvent.getCommission().getType();

            if (!"on-close".equals(commissionType) && !"on-purchase".equals(commissionType)) {
                throw new IllegalArgumentException("Event '" + name + "' has an invalid commission type.");
            }

            if (gmEvent.getGMOptions() == null
                    || gmEvent.getGMOptions().getGMOption() == null
                    || gmEvent.getGMOptions().getGMOption().size() != 2) {
                throw new IllegalArgumentException("Event '" + name + "' must have exactly two options.");
            }

            List<String> optionNames = gmEvent.getGMOptions().getGMOption();

            Set<String> uniqueOptions = new HashSet<>();

            for (String optionName : optionNames) {
                if (optionName == null || optionName.isBlank()) {
                    throw new IllegalArgumentException("Event '" + name + "' contains an empty option name.");
                }

                if (!uniqueOptions.add(optionName.trim().toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("Event '" + name + "' contains duplicate option names.");
                }
            }

            if (gmEvent.getGMMethod() == null) {
                throw new IllegalArgumentException("Event '" + name + "' is missing GM-method.");
            }

            boolean isLMSR = gmEvent.getGMMethod().getGMLMSR() != null;

            boolean isOrderBook = gmEvent.getGMMethod().getGMOrderBook() != null;

            if (isLMSR == isOrderBook) {
                throw new IllegalArgumentException("Event '" + name + "' must define exactly one trading method.");
            }

            if (isLMSR) {int b = gmEvent.getGMMethod().getGMLMSR().getB();

                if (b <= 0) {
                    throw new IllegalArgumentException("Event '" + name + "' must have LMSR b greater than 0.");
                }
            }

            if (isOrderBook) {
                var orderBook = gmEvent.getGMMethod().getGMOrderBook();

                if (orderBook.getD() <= 0) {throw new IllegalArgumentException("Event '" + name + "' must have Order Book d greater than 0.");
                }

                if (orderBook.getInitial() < 0) {
                    throw new IllegalArgumentException("Event '" + name + "' has a negative initial amount.");
                }

                if (!"true".equals(orderBook.getAllowMint()) && !"false".equals(orderBook.getAllowMint())) {
                    throw new IllegalArgumentException("Event '" + name + "' has an invalid allow-mint value.");
                }
            }
        }
    }


    private void validateEventNames(GuessMarket guessMarket) {
        Set<String> existingNames = events.stream()
                .map(Event::getEventName)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        Set<String> newNames = new HashSet<>();

        for (GMEvent gmEvent : guessMarket.getGMEvents().getGMEvent()) {
            String name = gmEvent.getName();

            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException(
                        "Event name cannot be empty.");
            }

            String normalizedName = name.trim().toLowerCase(Locale.ROOT);

            if (!newNames.add(normalizedName)) {
                throw new IllegalArgumentException(
                        "Duplicate event name in uploaded file: " + name);
            }

            if (existingNames.contains(normalizedName)) {
                throw new IllegalArgumentException(
                        "Event already exists on server: " + name);
            }
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
        List<Event> newEvents = new ArrayList<>();

        int nextId = eventIdGenerator.peekNextId();

        for (GMEvent gmEvent : guessMarket.getGMEvents().getGMEvent()) {
            Event event = new Event(gmEvent, nextId++);
            newEvents.add(event);
        }

        // Commit only when every event was constructed.
        events.addAll(newEvents);
        eventIdGenerator.advanceBy(newEvents.size());
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
                throw new IllegalArgumentException(
                        "Side (BUY/SELL) is required for order book trading"
                );
            }

            if (price == null) {
                throw new IllegalArgumentException(
                        "Price is required for order book trading"
                );
            }

            // Negative balance blocks BUY, but SELL is still allowed
            if (user.getAccountBalance() < 0 && side == Side.BUY) {
                throw new IllegalArgumentException(
                        "User with name " + userName + " cannot buy while account balance is negative"
                );
            }
            validateSharesQuantity(userName, optionNumber, shares, side, event);

        } else {
            // LMSR is always a BUY, so negative balance blocks participation
            if (user.getAccountBalance() < 0) {

                throw new IllegalArgumentException(
                        "User with name " + userName + " cannot participate while account balance is negative");
            }

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

    }

    @Override
    public void closeEvent(String userName, int eventId, int winningOption) {
        Event event = events.stream()
                .filter(e -> e.getId() == eventId)
                .findFirst()
                .orElse(null);

        if (event == null) {
            throw new IllegalArgumentException(
                    "Event with ID " + eventId + " not found"
            );
        }

        User user = users.get(userName);

        validateCloseEventParams(userName, eventId, winningOption, user, event);

        event.getMethod().close(
                event,
                users,
                winningOption
        );
    }

    private static void validateCloseEventParams(String userName, int eventId, int winningOption, User user, Event event) {
        if (user == null) {
            throw new IllegalArgumentException(
                    "User with name " + userName + " not found"
            );
        }

        if (!user.isMarketMaker(eventId)) {
            throw new IllegalArgumentException(
                    "User with name " + userName +
                            " is not the market maker for event ID " + eventId
            );
        }

        if (!event.isOpen()) {
            throw new IllegalStateException(
                    "Event with ID " + eventId + " is not open"
            );
        }

        if (winningOption < 1 ||
                winningOption > event.getOptions().size()) {

            throw new IllegalArgumentException(
                    "Invalid winning option: " + winningOption
            );
        }
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

        double activationCost = event.getMethod().getActivationCost();

        if (user.getAccountBalance() < activationCost) {
            throw new IllegalArgumentException(
                    "Insufficient funds. Event activation requires "
                            + String.format("%.2f", activationCost)
                            + ", but your balance is "
                            + String.format("%.2f", user.getAccountBalance())
            );
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


    @Override
    public User deposit(String userName, double amount) {

        User user = users.get(userName);

        if (user == null) {
            throw new IllegalArgumentException(
                    "User with name " + userName + " not found"
            );
        }

        if (!Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException(
                    "Deposit amount must be greater than 0"
            );
        }

        user.adjustBalance(amount, null, "Deposit");

        return user;
    }
}