package engine;

import java.io.Serializable;

/**
 * @param commissionPaid shares cost + commission
 */
public record Trade(
        String userName,
        String optionName,
        Side side,
        int sharesBought,
        double pricePaid,
        double commissionPaid
) implements Serializable
{
    private static final long serialVersionUID = 1L;

    public Trade(
            String userName, String optionName, int sharesBought,
            double pricePaid, Side buy, double commission) {
        this(userName, optionName, buy, sharesBought, pricePaid, commission);
    }

}