package engine;
import java.io.File;

import generated.GMEvents;
import generated.GuessMarket;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

import java.util.List;

public class EngineImpl implements Engine {
    List<Event> events;


    @Override
    public void loadFile(String path) throws JAXBException {
        //path is validated by ui so we can assume it is valid
        JAXBContext jaxbContext = JAXBContext.newInstance(GuessMarket.class);
        GuessMarket guessMarket = (GuessMarket) jaxbContext.createUnmarshaller().unmarshal(new File(path));
        guessMarket.getGMEvents().getGMEvent().forEach(gmEvent -> {
            events.add(new Event(gmEvent));
        });
    }

    @Override
    public void getEvents() {
        for (Event e : events) {

        }

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


//    @Override
//    public void exit() {
//        System.out.println("exit");
//    }
}
