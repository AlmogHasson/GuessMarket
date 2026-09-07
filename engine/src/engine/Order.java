package engine;

public class Order {
    private final long id;
    private final String userName;
    private final Side side;
    private int quantity;      // remaining, mutated as it fills
    private final double price;
    private final long seq;// insertion order → FIFO at equal price

    // ctor + getters/decrement
    public Order(long id, String userName, Side side, int quantity, double price, long seq) {
        this.id = id;
        this.userName = userName;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.seq = seq;
    }

    // getters
    public long getId() {
        return id;
    }

    public String getUserName() {
        return userName;
    }

    public Side getSide() {
        return side;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }

    public long getSeq() {
        return seq;
    }

    public void decrementQuantity(int qty) {
        quantity -= qty;
    }

    public void reduce(int qty) {
        if (qty > quantity) {
            throw new IllegalArgumentException("Cannot reduce quantity below zero");
        }
        quantity -= qty;
    }
}