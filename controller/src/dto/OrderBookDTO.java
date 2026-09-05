package dto;

public record OrderBookDTO(
        int d,
        int initial,
        String allowMint
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

    public int getInitial() {
        return initial;
    }

    public String getAllowMint() {
        return allowMint;
    }
}
