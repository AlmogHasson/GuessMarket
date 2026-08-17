package dto;
import java.util.List;
import engine.Event;

public record EventSummaryDTO( // record = immutable data class
       int id,
       String description,
       CommissionDTO comission,
       List<OptionDTO> options,
       MethodDTO method,
       String name,
       boolean isOpen
) {
    // Constructor to create dto.EventDTO from Event
    public EventSummaryDTO(Event event) {
        this(
            event.getId(),
            event.getDescription(),
            new CommissionDTO(event.getComission()),
            event.getOptions().stream().map(OptionDTO::new).toList(),
            new MethodDTO(new LMSRDTO(event.getMethod().getLmsr())),
            event.getName(),
            event.getEventTradingStatus().isOpen()
        );
    }

    //getters
    public int getId() {
        return id;
    }

    public boolean isOpen(){
        return isOpen;
    }

    public String getDescription() {
        return description;
    }

    public CommissionDTO getCommission() {
        return comission;
    }

    public List<OptionDTO> getOptions() {
        return options;
    }

    public MethodDTO getMethod() {
        return method;
    }

    public String getName() {
        return name;
    }
}