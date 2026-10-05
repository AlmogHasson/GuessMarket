package engine;

import jakarta.xml.bind.JAXBException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.io.IOException;


public interface Engine {
    void loadFile(String path) throws JAXBException;
    List<Event> getEvents();
    User getOrCreateUser(String userName);
    Map<String, User> getUsers();
    EventTradingStatus getEventTradingStatus(int eventId);
    Purchase participateInEvent(String userName,int eventId, int optionNumber, int shares,Side side, Double price);
    void closeEvent(int eventID,int winningOption);
    void activateEvent(String name, int eventId);
    List<String> getEventParticipants(int eventId);
    int getParticipantShares(int eventId, String userName, int optionNumber);
}