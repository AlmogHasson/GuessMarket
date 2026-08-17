package engine;

import jakarta.xml.bind.JAXBException;

import java.util.List;

public interface Engine {
    void loadFile(String path) throws JAXBException;
    List<Event> getEvents();
    //TODO: check if events are displayed LIFO
    //TODO: check if when closed displays the winner option
    EventTradingStatus getEventTradingStatus(int eventId);
    Purchase participateInEvent(int eventId, int optionNumber, int shares); //place a bet
    void closeEvent();
}
