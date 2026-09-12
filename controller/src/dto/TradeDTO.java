package dto;

import engine.Trade;

public record TradeDTO(
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
                trade.userName(),
                Side.fromEngine(trade.side()),
                trade.optionName(),
                trade.sharesBought(),
                trade.pricePaid(),
                trade.commissionPaid());
    }
}
