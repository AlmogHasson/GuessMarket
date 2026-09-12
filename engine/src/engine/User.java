package engine;

import java.util.List;

public class User {
    protected double accountBalance; // initialCash in the generated files
    protected List<Integer> eventsIDs;
    protected String name;

    public double getAccountBalance() {
        return accountBalance;
    }

    public void setAccountBalance(double value) {
        this.accountBalance = value;
    }

    public List<Integer> getUserEvents() {
        return eventsIDs;
    }

    public void setUserEvents(List<Integer> value) {
        this.eventsIDs = value;
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
}
