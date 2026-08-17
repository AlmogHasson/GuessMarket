package dto;

import engine.OptionTradingStatus;

public record OptionTradingDTO(
        String optionName,
        float currentValue,
        int totalSharesBought
    )
{
    public OptionTradingDTO(OptionTradingStatus optionTradingStatus) {
        this(optionTradingStatus.getOptionName(),
                optionTradingStatus.getCurrentValue(),
                optionTradingStatus.getTotalSharesBought());
    }
}
