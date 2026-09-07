package engine;

import java.io.Serializable;

public class Trade implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String userName;
    private final String optionName;
    private final Side side;
    private final int sharesBought;
    private final double pricePaid; /** shares cost + commission */
    private final double commission;

    public Trade(
            String userName, String optionName, int sharesBought,
            double pricePaid, Side buy, double commission)
    {
        this.userName = userName;
        this.optionName = optionName;
        this.sharesBought = sharesBought;
        this.pricePaid = pricePaid;
        this.side = buy;
        this.commission = commission;
    }

    // Getters and setters
    public String getOptionName() {
        return optionName;
    }

    public String getUserName() {
        return userName;
    }

    public int getSharesBought() {
        return sharesBought;
    }

    public double getPricePaid() {
        return pricePaid;
    }

    public Side getSide() {
        return side;
    }

    public double getCommission() {
        return commission;
    }

}