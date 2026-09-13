package dto;

import engine.Trade;
import java.util.UUID;

public record TradeDTO(
        UUID id,
        String userName,
        Side side,
        String optionName,
        int sharesBought,
        double pricePaid,
        double commissionPaid
    )
{
    public TradeDTO (Trade trade) {
        this(
                trade.id(),
                trade.userName(),
                Side.fromEngine(trade.side()),
                trade.optionName(),
                trade.sharesBought(),
                trade.pricePaid(),
                trade.commissionPaid());
    }

    public double executionPrice() {
        if (sharesBought == 0) {
            return 0.0;
        }

        // Order-book BUY pricePaid includes commission.
        // SELL pricePaid contains the proceeds received.
        double gross = side == Side.BUY
                ? pricePaid - commissionPaid
                : pricePaid;

        return gross / sharesBought;
    }
}
