import java.util.concurrent.ConcurrentLinkedQueue;

public class MultiQ extends LamportQ{
    ConcurrentLinkedQueue<Packet> queue;

    public MultiQ() {
        super(1024);
        queue = new ConcurrentLinkedQueue<>();

    }
    public void enq(Packet x) throws FullException {
        queue.add(x);
    }

    public Packet deq() throws EmptyException {
        Packet result = queue.poll();
        if (result == null) {
            throw new EmptyException();
        }
        return result;
    }

    
}
