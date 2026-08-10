package engine;

public class EngineImpl implements Engine {
//    List<Event> events;

    @Override
    public void loadFile() {
        System.out.println("in load");
    }

    @Override
    public void getEvents() {
        System.out.println("in get events");
    }

    @Override
    public void getEventTradingStatus() {
        System.out.println("in getEventTradingStatus");
    }

    @Override
    public void participateInEvent() {
        System.out.println("in participateInEvent");
    }

    @Override
    public void closeEvent() {
        System.out.println("in closeEvent");
    }

    @Override
    public void exit() {
        System.out.println("exit");
    }
}
