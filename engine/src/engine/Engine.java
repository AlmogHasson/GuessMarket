package engine;

import jakarta.xml.bind.JAXBException;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.io.IOException;


public interface Engine {
//    void loadFile(String path) throws JAXBException;
    void loadFile(InputStream inputStream) throws JAXBException;
    List<Event> getEvents();
    User getOrCreateUser(String userName);
    Map<String, User> getUsers();
    EventTradingStatus getEventTradingStatus(int eventId);
    Purchase participateInEvent(String userName,int eventId, int optionNumber, int shares,Side side, Double price);
    void closeEvent(String userName, int eventId, int winningOption);
    void activateEvent(String name, int eventId);
    List<String> getEventParticipants(int eventId);
    int getParticipantShares(int eventId, String userName, int optionNumber);
    User deposit(String userName, double amount);
}