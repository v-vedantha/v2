// import arraylist
import java.util.ArrayList;
public class Dispatcher implements Runnable {
    PacketGenerator source;
    LamportQ[] queue;
    PaddedPrimitiveNonVolatile<Boolean> done;

    public Dispatcher(PacketGenerator source, LamportQ[] queue, PaddedPrimitiveNonVolatile<Boolean> done) {
        // Initialize Dispatcher
        this.source = source;
        this.queue = queue;
        this.done = done;
    }

    public void run() {
        int i = 0;
        // We'll just hash by source address
        while (!done.value) {
            // Add packets to queue
            // System.out.println("Adding packet to queue" + i % queue.length);
            Packet pkt = source.getPacket();
            int idx;
            boolean succ = false;
            while (!succ && !done.value) {
                // System.out.println("done is " + done.value);
                try {
                    switch (pkt.type) {
                        case DataPacket:
                            idx = Math.floorMod(pkt.header.source, queue.length);
                            queue[idx].enq(pkt);
                            succ = true;
                            break;
                        case ConfigPacket:
                            idx = Math.floorMod(pkt.config.address, queue.length);
                            queue[idx].enq(pkt);
                            // System.out.println("Its an remove packet" + myidx);
                            succ= true;
                            break;
                    }
                } catch (FullException e) {
                    // System.out.println("Queue is full");
                }
            }
        }
        

    }
    
}
