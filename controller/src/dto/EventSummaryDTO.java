package dto;
import java.util.List;
import engine.Event;

public record EventSummaryDTO(
       int id,
       String description,
       CommissionDTO commission,
       List<OptionDTO> options,
       MethodDTO method,
       String name,
       EventStatus status
) {
    // Constructor to create dto.EventDTO from Event
    public EventSummaryDTO(Event event) {
        this(
            event.getId(),
            event.getDescription(),
            new CommissionDTO(event.getCommission()),
//            event.getOptions().stream().map(OptionDTO::new).toList(),
            event.getOptions().stream()
                    .map(option -> (OptionDTO) (event.getMethod() instanceof engine.LMSR
                            ? new LMSROptionDTO(option)
                            : new OBOptionDTO(option, ((engine.OrderBook) event.getMethod())
                            .getRestingOrders(option.getOptionNumber()))))
                    .toList(),
            (event.getMethod() instanceof engine.LMSR
            ? new LMSRDTO((engine.LMSR) event.getMethod())
            : new OrderBookDTO((engine.OrderBook) event.getMethod())),
            event.getEventName(),
            EventStatus.from(event.getEventTradingStatus().getStatus())
        );
    }

    //getters
    public int getId() {
        return id;
    }

    public boolean isOpen(){
        return status == EventStatus.OPEN;
    }

    public EventStatus getStatus(){
        return status;
    }

    public String getDescription() {
        return description;
    }

    public CommissionDTO getCommission() {
        return commission;
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