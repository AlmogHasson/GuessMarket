package dto;

public record OrderBookDTO(
        int d,
        int initial,
        boolean allowMint
) implements MethodDTO {

    // Constructor to create OrderBookDTO from OrderBook
    public OrderBookDTO(engine.OrderBook orderBook) {
        this(
                orderBook.getValue(),
                orderBook.getInitial(),
                orderBook.getAllowMint()
        );
    }

    @Override
    public String getName() {
        return "order book";
    }

    @Override
    public int getValue() {
        return d;
    }

}
