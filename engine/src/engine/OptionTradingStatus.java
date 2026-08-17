package engine;

public class OptionTradingStatus {
    private String optionName;
    private float currentValue;   // 0..1
    private int totalSharesBought;

    public OptionTradingStatus(Option option) {
        this.optionName = option.getName();   // from domain Option
        this.currentValue = 0.0f;
        this.totalSharesBought = option.getSharesBought();
    }

    // Getters
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
