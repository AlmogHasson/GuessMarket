package dto;

import engine.Order;

public record OrderDTO(
          long id,
          String userName,
          Side side,
          int quantity,      // remaining, mutated as it fills
          double price,
          long seq
)
{

    public OrderDTO(Order order) {
        this(
                order.getId(),
                order.getUserName(),
                Side.fromEngine(order.getSide()),
                order.getQuantity(),
                order.getPrice(),
                order.getSeq());
    }
}
