package engine;

import java.io.Serial;
import java.io.Serializable;

public class Option implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private int optionNumber; // 1 or 2
    private String optionName;
    private double currentValue;   // 0..1
    private int totalSharesBought;
    boolean isWinner;

    public Option(String optionName, int optionNumber) {
        this.optionName = optionName;
        this.currentValue = 0.5f;  // Default to 50% chance
        this.totalSharesBought = 0;
        this.isWinner = false;
        this.optionNumber = optionNumber;
    }

    // Getters
    public String getOptionName() {
        return optionName;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public int getTotalSharesBought() {
        return totalSharesBought;
    }

    public void buyShares(int shares) {
        totalSharesBought += shares;
    }

    public void updateValue(double newValue) {
        currentValue = newValue;
    }

    public boolean getIsWinner() {
        return isWinner;
    }

    public void setWinner() {
        isWinner = true;
    }

    public int getOptionNumber() {
        return optionNumber;
    }

}
