package api;

import dto.EventDTO;
import engine.Engine;
import engine.Event;
import generated.GuessMarket;
import jakarta.xml.bind.JAXBException;

import java.io.File;
import java.util.List;

public class GMController {
    private Engine engine;


    public void loadFile(String path) throws JAXBException {
        engine.loadFile(path);
    }

    public List<EventDTO> getEvents() {
        return engine.getEvents().stream().map(event -> new EventDTO(event)).toList();
    }

    public void getEventTradingStatus() {
        engine.getEventTradingStatus();
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
