package dto;

import ComissionDTO;
import MethodDTO;
import OptionDTO;

import java.util.List;

public record  EventDTO( // record = immutable data class
    int id,
    String description,
    Comission comission,
    List<Option> options,
    Method method,
    String name
) {

    // Constructor to create dto.EventDTO from Event
    public EventDTO(engine.Event event) {
        this(
            event.getId(),
            event.getDescription(),
            event.getComission(),
            event.getOptions(),
            event.getMethod(),
            event.getName()
        );
    }

    //implement all getters for the fields
    public int id() {
        return id;
    }

    public String description() {
        return description;
    }

    public Comission comission() {
        return comission;
    }

    public List<Option> options() {
        return options;
    }

    public Method method() {
        return method;
    }

    public String name() {
        return name;
    }
}