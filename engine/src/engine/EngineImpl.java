package engine;
import java.io.File;

import generated.GMEvent;
import generated.GuessMarket;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EngineImpl implements Engine {
    List<Event> events;


    @Override
    public void loadFile(String path) throws JAXBException {
        // validate the file path : exists, readable, not null or empty
        validateFilePath(path);

        JAXBContext jaxbContext = JAXBContext.newInstance(GuessMarket.class);
        GuessMarket guessMarket = (GuessMarket) jaxbContext.createUnmarshaller().unmarshal(new File(path));

        validateIds(guessMarket);
        // check if commission is between 0 and 90
        validateCommissions(guessMarket);

        //when the file is valid, load the events into the events list
        loadEvents(guessMarket);

    }

    private void loadEvents(GuessMarket guessMarket) {
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
    public Purchase participateInEvent(int eventId, int optionNumber, int shares) {
        Event event = events.stream().filter(e -> e.getId() == eventId).findFirst().orElse(null);

        if (!event.isOpen()){
            return null;
        }

        if (event == null) {
            throw new IllegalArgumentException("Event with ID " + eventId + " not found");
        }
        if (optionNumber < 0 || optionNumber > event.getOptions().size()) {
            throw new IllegalArgumentException("Invalid option number: " + optionNumber);
        }
        if (shares <= 0) {
            throw new IllegalArgumentException("Shares must be greater than 0");
        }

        return event.participate(optionNumber, shares);
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


        float winningShares = event.getOptions().get(winningOption-1).getTotalSharesBought();
        String commissionType = event.getComission().getType();
        float commission = commissionType.equals("on-close")
                ? winningShares * event.getComission().getValue() / 100 : 0.0f;

        ETS.updateTotalCommissionPaid(ETS.getTotalCommissionPaid() + commission);

        float payOut = winningShares - commission;
        ETS.updateAccountBalance(ETS.getAccountBalance() - payOut);

    }


    private void validateCommissions(GuessMarket guessMarket) {
        var invalidIds = guessMarket.getGMEvents().getGMEvent().stream()
                .filter(e -> {
                    int commission = e.getCommission();
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

}
