package engine;

import jakarta.xml.bind.JAXBException;

import java.util.List;
import java.util.Optional;

public interface Engine {
    void loadFile(String path) throws JAXBException;
    List<Event> getEvents();
    EventTradingStatus getEventTradingStatus(int eventId);
    Purchase participateInEvent(int eventId, int optionNumber, int shares); //place a bet
    void closeEvent(int eventID,int winningOption);
}
