package dto;

import engine.Trade;

public record TradeDTO(
        String optionName,
        int sharesBought,
        double pricePaid
    )
{
    public TradeDTO (Trade trade) {
        this(trade.getOptionName(), trade.getSharesBought(), trade.getPricePaid());
    }
}
