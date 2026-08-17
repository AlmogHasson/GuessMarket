package engine;
import java.io.File;

import generated.GMEvent;
import generated.GuessMarket;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

import java.util.ArrayList;
import java.util.List;

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
    public void participateInEvent() {
        System.out.println("in participateInEvent");
    }

    @Override
    public void closeEvent() {
        System.out.println("in closeEvent");
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
