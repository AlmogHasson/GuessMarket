package dto;

import engine.Trade;

public record TradeDTO(
        String userName,
        String optionName,
        int sharesBought,
        double pricePaid
    )
{
    public TradeDTO (Trade trade) {
        this(trade.getUserName(), trade.getOptionName(), trade.getSharesBought(), trade.getPricePaid());
    }
}
