package engine;

public class EventIdGenerator {
    private int id = 1;

    public int getIdAndIncrement() {
        return id++;
    }


    public void advanceBy(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException(
                    "Amount cannot be negative");
        }
        id += amount;
    }

    public int peekNextId() {
        return id;
    }


}
