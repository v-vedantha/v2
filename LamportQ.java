
class FullException extends Exception {
    public FullException() {
        super("Queue is full");
    }
}
class EmptyException extends Exception {
    public EmptyException() {
        super("Queue is empty");
    }
}
public class LamportQ {
    volatile int head = 0, tail = 0;
    Packet[] items;

    public LamportQ(int capacity) {
        items = new Packet[capacity*1024];
        head = 0;
        tail = 0;

    }
    public void enq(Packet x) throws FullException {
        if (tail - head == items.length)
            throw new FullException();
        items[tail % items.length] = x;
        tail++;
    }

    public Packet deq() throws EmptyException {
        if (tail - head == 0)
            throw new EmptyException();
        Packet x = items[head % items.length];
        head++;
        return x;
    }
}
