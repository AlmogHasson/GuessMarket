package dto;

import engine.OrderBook;

public record OrderBookStatsDTO(
        Double last, Double bestBid, Double bestAsk, Double mid, Double spread
)
{
    public OrderBookStatsDTO(OrderBook orderBook, int optionNumber)
    {
        this(
                orderBook.getLast(optionNumber),
                orderBook.getBestBid(optionNumber),
                orderBook.getBestAsk(optionNumber),
                orderBook.getMid(optionNumber),
                orderBook.getSpread(optionNumber));
    }
}