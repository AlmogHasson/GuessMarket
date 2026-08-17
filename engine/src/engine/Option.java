package engine;

public class Option {
    private String optionName;
    private float currentValue;   // 0..1
    private int totalSharesBought;

    public Option(String optionName) {
        this.optionName = optionName;
        this.currentValue = 0.0f;
        this.totalSharesBought = 0;
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
