package engine;

import java.util.ArrayList;
import java.util.List;

public class User {
    protected double accountBalance; // initialCash in the generated files
    protected List<Integer> eventsIDs = new java.util.ArrayList<>(); // list of event IDs where the user is a market maker
    protected String name;
    private final List<AccountTransaction> accountTransactions = new ArrayList<>();
    private long nextTransactionId = 1;

    public double getAccountBalance() {
        return accountBalance;
    }

    public void setAccountBalance(double value) {
        this.accountBalance = value;
    }

    public List<Integer> getUserEvents() {
        return eventsIDs;
    }

    public List<AccountTransaction> getAccountTransactions() {
        return List.copyOf(accountTransactions);
    }

    public void addUserEvent(int eventID) {
        this.eventsIDs.add(eventID);
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public boolean isMarketMaker(int eventID) {
        return eventsIDs.contains(eventID);
    }

    public void adjustBalance(double amount, String eventName, String activity) {
        accountBalance += amount;

        accountTransactions.add(new AccountTransaction(
                        nextTransactionId++,
                        eventName,
                        activity,
                        amount,
                        accountBalance
                )
        );
    }

}