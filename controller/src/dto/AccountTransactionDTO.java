package dto;

import engine.AccountTransaction;

public record AccountTransactionDTO(
        long id,
        String eventName,
        String activity,
        double amount,
        double balanceAfter
) {
    public AccountTransactionDTO(AccountTransaction transaction) {
        this(
                transaction.id(),
                transaction.eventName(),
                transaction.activity(),
                transaction.amount(),
                transaction.balanceAfter()
        );
    }
}