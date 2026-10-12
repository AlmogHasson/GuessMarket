package engine;

public record AccountTransaction(
        long id,
        String eventName,
        String activity,
        double amount,
        double balanceAfter
) {
}