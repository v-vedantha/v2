import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MultiQ extends LamportQ{
    BlockingQueue<Packet> queue;

    public MultiQ() {
        super(1024);
        queue = new LinkedBlockingQueue<>(1000);

    }
    public void enq(Packet x) throws FullException {
        try { queue.add(x);} catch (IllegalStateException e) {throw new FullException();}
    }

    public Packet deq() throws EmptyException {
        Packet result = queue.poll();
        if (result == null) {
            throw new EmptyException();
        }
        return result;
    }

    
}
