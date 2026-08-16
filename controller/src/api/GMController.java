package api;

import dto.EventDTO;
import engine.Engine;
import engine.EngineImpl;

import java.util.List;

public class GMController {
    private Engine engine = new EngineImpl();

    public void loadFile(String path) throws Exception {
        engine.loadFile(path);
    }

    public List<EventDTO> getEvents() {
        return engine.getEvents().stream().map(event -> new EventDTO(event)).toList();
    }

    public void getEventTradingStatus(int eventId) {
        engine.getEventTradingStatus(eventId);
    }

    public void participateInEvent() {
        engine.participateInEvent();
    }

    public void closeEvent() {
        engine.closeEvent();
    }


//    @Override
//    public void exit() {
//        System.out.println("exit");
//    }

}
