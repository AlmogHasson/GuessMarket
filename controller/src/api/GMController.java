package api;

import dto.EventSummaryDTO;
import dto.EventTradingStatusDTO;
import engine.Engine;
import engine.EngineImpl;

import java.util.List;

public class GMController {
    private Engine engine = new EngineImpl();

    public void loadFile(String path) throws Exception {
        engine.loadFile(path);
    }

    public List<EventSummaryDTO> getEvents() {
        return engine.getEvents().stream().map(event -> new EventSummaryDTO(event)).toList();
    }

    public EventTradingStatusDTO getEventTradingStatus(int eventId) throws IllegalArgumentException {
        return new EventTradingStatusDTO(
                // get the event by ID and create a DTO from it
                engine.getEvents().stream().filter(e ->
                                e.getId() == eventId)
                                .findFirst().orElseThrow(() ->
                                new IllegalArgumentException("Event with ID " + eventId + " not found"))
        );
    }

    public void participateInEvent() {
        engine.participateInEvent();
    }

    public void closeEvent() {
        engine.closeEvent();
    }

    public boolean isFileLoaded() {
        return engine.getEvents() != null && !engine.getEvents().isEmpty();
    }


//    @Override
//    public void exit() {
//        System.out.println("exit");
//    }

}
