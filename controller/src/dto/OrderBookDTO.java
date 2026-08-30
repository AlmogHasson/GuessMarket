package dto;

public record OrderBookDTO() implements MethodDTO {
    // Constructor to create OrderBookDTO from OrderBook
    public OrderBookDTO(engine.OrderBook orderBook) {
        this();
    }

    @Override
    public int getValue() {
        return 0; // OrderBook does not have a value, return 0 or any other default value
    }
    @Override
    public String getName() {
        return "order book";
    }
}
