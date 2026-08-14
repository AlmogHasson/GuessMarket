package engine;

import jakarta.xml.bind.JAXBException;

import java.util.List;

public interface Engine {
    void loadFile(String path) throws JAXBException;
    void getEvents();
    void getEventTradingStatus();
    void participateInEvent(); //place a bet
    void closeEvent();
    //void exit(); is exited/killed by garbage collector when ui dies-when we close the program

}
