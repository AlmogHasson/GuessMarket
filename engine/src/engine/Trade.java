package engine;

public class Trade {
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