package api;

import dto.EventSummaryDTO;
import dto.EventTradingStatusDTO;
import dto.PurchaseDTO;
import engine.Engine;
import engine.EngineImpl;

import java.util.List;
import java.util.Optional;
import java.io.IOException;

public class GMController {
    private Engine engine = new EngineImpl();

    public void loadFile(String path) throws Exception {
        engine.loadFile(path);
    }

    public List<EventSummaryDTO> getEvents() {
        return engine.getEvents().stream().map(event -> new EventSummaryDTO(event)).toList();
    }

    public EventTradingStatusDTO getEventTradingStatus(int eventId) throws IllegalArgumentException {
        return new EventTradingStatusDTO(engine.getEventTradingStatus(eventId));
    }

    public PurchaseDTO participateInEvent(int eventId, int optionNumber, int shares) {
        return new PurchaseDTO(engine.participateInEvent(eventId, optionNumber, shares));
    }

    public void closeEvent(int eventID,int winningOption) {
        engine.closeEvent(eventID, winningOption);
    }

    public boolean isFileLoaded() {
        return engine.getEvents() != null && !engine.getEvents().isEmpty();
    }

    //bonus: save and load the state of the engine to a file
    public void saveState(String path) throws IOException {
        engine.saveState(path);
    }

    public void loadState(String path) throws IOException, ClassNotFoundException {
        engine.loadState(path);
    }

}
