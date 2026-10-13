package engine;

import java.io.Serializable;
import java.util.UUID;

public record Trade(
        UUID id,
        String userName,
        String optionName,
        Side side,
        int sharesBought,
        double pricePaid,
        double commissionPaid
) implements Serializable {


    public Trade {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public Trade(
            String userName,
            String optionName,
            Side side,
            int sharesBought,
            double pricePaid,
            double commissionPaid
    ) {
        this(
                UUID.randomUUID(),
                userName,
                optionName,
                side,
                sharesBought,
                pricePaid,
                commissionPaid
        );
    }

    public Trade(
            String userName,
            String optionName,
            int sharesBought,
            double pricePaid,
            Side side,
            double commissionPaid
    ) {
        this(
                userName,
                optionName,
                side,
                sharesBought,
                pricePaid,
                commissionPaid
        );
    }
}