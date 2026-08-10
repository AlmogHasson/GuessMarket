package engine;

public interface Engine {
    void loadFile();
    void getEvents();
    void getEventTradingStatus();
    void participateInEvent(); //place a bet
    void closeEvent();
    void exit();
}
