package engine;

import generated.GMEvent;
import generated.GMMethod;
import generated.GMOptions;

import java.util.List;

public class Event {
    private int id;
    private String description;
    private Comission comission;
    private List<Option> options;
    private Method method;
    private String name;

    //get the event from schema and load it with the ctor
    public Event(GMEvent event) {
    }
}
