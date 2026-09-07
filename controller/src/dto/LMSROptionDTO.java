package dto;

import engine.Option;

public record LMSROptionDTO (
        String optionName,
        double currentValue,
        int totalSharesBought,
        boolean isWinner
    ) implements OptionDTO {

    public LMSROptionDTO(Option option) {
        this(option.getOptionName(),
                option.getCurrentValue(),
                option.getTotalSharesBought(),
                option.getIsWinner());
    }

    //getters
    public String getOptionName() {
        return optionName;
}

    public double getCurrentValue() {
        return currentValue;
    }

    public int getTotalSharesBought() {
        return totalSharesBought;
    }

}
