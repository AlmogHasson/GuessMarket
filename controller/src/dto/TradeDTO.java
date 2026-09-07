package dto;

import engine.Trade;

public record TradeDTO(
        String userName,
        Side side,
        String optionName,
        int sharesBought,
        double pricePaid
    )
{
    public TradeDTO (Trade trade) {
        this(
                trade.getUserName(),
                Side.fromEngine(trade.getSide()),
                trade.getOptionName(),
                trade.getSharesBought(),
                trade.getPricePaid());
    }
}
