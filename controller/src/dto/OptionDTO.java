package dto;

import engine.Option;

public record OptionDTO(
        String optionName,
        float currentValue,
        int totalSharesBought,
        boolean isWinner
    )
{
    public OptionDTO(Option option) {
        this(option.getOptionName(),
                option.getCurrentValue(),
                option.getTotalSharesBought(),
                option.getIsWinner());
    }

    //getters
    public String getOptionName() {
        return optionName;
}

    public float getCurrentValue() {
        return currentValue;
    }

    public int getTotalSharesBought() {
        return totalSharesBought;
    }

}
