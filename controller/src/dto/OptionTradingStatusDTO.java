package dto;

public record OptionTradingStatusDTO(String optionName,
                                     double currentValue,
                                     int sharesBought) {

}
