package engine;

import java.io.Serializable;

public class Trade implements Serializable {
    private static final long serialVersionUID = 1L;

    private String optionName;
    private int sharesBought;
    private double pricePaid;

    public Trade(String optionName, int sharesBought, double pricePaid) {
        this.optionName = optionName;
        this.sharesBought = sharesBought;
        this.pricePaid = pricePaid;
    }

    // Getters and setters
    public String getOptionName() {
        return optionName;
    }

    public int getSharesBought() {
        return sharesBought;
    }

    public double getPricePaid() {
        return pricePaid;
    }
}