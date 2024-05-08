
public class Worker implements Runnable {

    PacketProcessor p;
    long totalPackets = 0;
    LamportQ queue;
    static int idx;
    private int myidx = idx++;
    PaddedPrimitiveNonVolatile<Boolean> done;
    public Worker(PacketProcessor p, LamportQ queue, PaddedPrimitiveNonVolatile<Boolean> done) {
        this.p = p;
        this.done= done;
        this.queue = queue;
    }

    public void run() {
        System.out.println("Worker started" + myidx);
        while (!done.value) {
            // System.out.println("Worker running" + myidx);
            Packet pkt;
            try{
                pkt = queue.deq();
            }
            catch (Exception e) {
                continue;
            }
            totalPackets++;
            // System.out.println("Worker got a packet" + myidx);
            if (p == null) {
                continue;
            }
            switch(pkt.type) {
                case DataPacket: 
                    // System.out.println("its a data packet" + myidx);
                    p.processData(pkt.header, pkt.body);
                break;
                case ConfigPacket:
                    // System.out.println("its a config packet" + myidx);
                    p.processConfig(pkt.config);
                // System.out.println("Its an remove packet" + myidx);
                break;
            }
            // System.out.println("Worker solved a packet" + myidx);
        }
        System.out.println("Worker finished" + myidx);
    }
}
