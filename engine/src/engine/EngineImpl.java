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

    private void validateCommissions(GuessMarket guessMarket) {
        if (!areCommissionsValid(guessMarket.getGMEvents().getGMEvent())) {
            //TODO: include the invalid event IDs in the exception message
            throw new IllegalArgumentException("Commission must be between 0 and 90");
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
        // check if the file exists
        File file = new File(path);
        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist");
        }
        // check if the file is readable
        if (!file.canRead()) {
            throw new IllegalArgumentException("File is not readable");
        }
    }

    private boolean areCommissionsValid(List<GMEvent> gmEvents) {
        return gmEvents.stream().allMatch(event -> {
                int commission = event.getCommission();
                return commission >= 0 && commission <= 90;
            });
    }


    public EngineImpl() {
        this.events = new ArrayList<>();
    }

    @Override
    public List<Event> getEvents() {
        return events;
    }

    @Override
    public void getEventTradingStatus(int eventId) {
        System.out.println("in getEventTradingStatus");
    }

    @Override
    public void participateInEvent() {
        System.out.println("in participateInEvent");
    }

    @Override
    public void closeEvent() {
        System.out.println("in closeEvent");
    }

//    @Override
//    public void exit() {
//        System.out.println("exit");
//    }

    private boolean hasUniqueIds(List<GMEvent> gmEvent) {
        return gmEvent.stream().map(GMEvent::getId).distinct().count() == gmEvent.size();
    }

}
