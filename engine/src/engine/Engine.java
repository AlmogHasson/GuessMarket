package engine;

import jakarta.xml.bind.JAXBException;

import java.util.List;
import java.util.Optional;
import java.io.IOException;


public interface Engine {
    void loadFile(String path) throws JAXBException;
    List<Event> getEvents();
    EventTradingStatus getEventTradingStatus(int eventId);
    Purchase participateInEvent(int eventId, int optionNumber, int shares); //place a bet
    void closeEvent(int eventID,int winningOption);
    //bonus part: save and load the state of the engine to a file
    void saveState(String path) throws IOException;
    void loadState(String path) throws IOException, ClassNotFoundException;
}
