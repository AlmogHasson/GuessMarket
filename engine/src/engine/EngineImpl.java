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

        //when the file is valid, load the events and users
        loadEvents(guessMarket);
        loadUsers(guessMarket);

    }

    @Override
    public Map<String, User> getUsers() {
        return users;
    }

    private void validateUsers(GuessMarket guessMarket) {
        // check if users have unique names
        var invalidNames = guessMarket.getGMUsers().getGMUser().stream()
                .map(GMUser::getName)
                .filter(name -> guessMarket.getGMUsers().getGMUser().stream()
                        .filter(u -> u.getName().equals(name))
                        .count() > 1)
                .distinct()
                .toList();
        if (!invalidNames.isEmpty()) {
            throw new IllegalArgumentException("Users with duplicate names found: " + invalidNames);
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
    public Purchase participateInEvent(String userName ,int eventId, int optionNumber, int shares) {
        Event event = events.stream().filter(e -> e.getId() == eventId).findFirst().orElse(null);
        User user = users.get(userName);

        if (user == null) {
            throw new IllegalArgumentException("User with name " + userName + " not found");
        }

        if (user.isEventMaker(eventId)) {
            throw new IllegalArgumentException("User with name " + userName + " is the market maker for event ID " + eventId);
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
            throw new IllegalArgumentException("User with name " + userName + " has insufficient funds");
        }

        Purchase purchase = event.participate(user, optionNumber, shares);
        user.setAccountBalance(user.getAccountBalance() - purchase.getTotalPaid());
        return purchase;
    }



    @Override
    public void closeEvent(int eventId,int winningOption)throws  IllegalArgumentException{
        Event event = events.stream().filter(e -> e.getId() == eventId).findFirst().orElse(null);
        if (event == null) {
            throw new IllegalArgumentException("Event with ID " + eventId + " not found");
        }
        if (winningOption < 0 || winningOption > event.getOptions().size()) {
            throw new IllegalArgumentException("Invalid option number: " + winningOption);
        }

        EventTradingStatus ETS = event.getEventTradingStatus();
        ETS.close();
        event.getOptions().get(winningOption-1).setWinner();

        double winningShares = event.getOptions().get(winningOption-1).getTotalSharesBought();
        String commissionType = event.getComission().getCommissionType();
        double commission = commissionType.equals("on-close")
                ? winningShares * event.getComission().getValue() / 100 : 0.0;

        ETS.updateTotalCommissionPaid(ETS.getTotalCommissionPaid() + commission);

        double payOut = winningShares - commission;
        ETS.updateAccountBalance(ETS.getAccountBalance() - payOut);

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
