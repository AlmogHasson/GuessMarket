package dto;

import engine.Option;
import engine.Order;
import java.util.List;

public record OBOptionDTO(
        String optionName,
        double currentValue,
        int totalSharesBought,
        boolean isWinner,
        List<OrderDTO> restingOrders

    ) implements OptionDTO
{
    public OBOptionDTO(Option option, List<Order> restingOrders) {
        this(option.getOptionName(),
                option.getCurrentValue(),
                option.getTotalSharesBought(),
                option.getIsWinner(),
                restingOrders.stream().map(OrderDTO::new).toList());
    }

    //getters
    public String getOptionName() {
        return optionName;
}

    public double getCurrentValue() {
        return currentValue;
    }

    public int getTotalSharesBought() {
        return totalSharesBought;
    }

}