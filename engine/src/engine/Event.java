package engine;

import generated.GMEvent;

import java.util.ArrayList;
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
        this.id = event.getId();
        this.description = event.getDescription();
        this.comission = new Comission(event.getComision());
        this.options = new ArrayList<>();
        event.getGMOptions().getGMOption().forEach(option ->
                this.options.add(new Option(option))
        );
        this.method = new Method(event.getGMMethod());
        this.name = String.join(", ", event.getName());
    }

    //getters
    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Comission getComission() {
        return comission;
    }

    public List<Option> getOptions() {
        return options;
    }

    public Method getMethod() {
        return method;
    }

    public String getName() {
        return name;
    }
}
