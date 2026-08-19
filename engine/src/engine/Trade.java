package engine;

import java.io.Serializable;

public class Trade implements Serializable {
    private static final long serialVersionUID = 1L;

    private String optionName;
    private int sharesBought;
    private float pricePaid;

    public Trade(String optionName, int sharesBought, float pricePaid) {
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

    public float getPricePaid() {
        return pricePaid;
    }
}