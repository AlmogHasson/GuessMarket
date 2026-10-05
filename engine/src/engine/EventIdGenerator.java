package engine;

public class EventIdGenerator {
    private int id = 1;

    public int getIdAndIncrement() {
        return id++;
    }

}
