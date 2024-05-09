public class BetterDispatcher implements Runnable {
    // We want to check the cache manually, and then enq into the relevent workers
    // They will then output the results onto a stream, which we join at the very end, and then re-enq if needed
    LamportQ input;
    AllData data;
    LamportQ[] cacheprocessors;
    public int packetsProcessed = 0;
    public int cachesenq = 0;

    PaddedPrimitiveNonVolatile<Boolean> done;
    // We get dispatched to via the dispatcher and potentially some workers.
    // Our entire thing will just enq into the relevent cache worker's queue
    public BetterDispatcher(LamportQ input, AllData data, LamportQ[] cacheprocessors, PaddedPrimitiveNonVolatile<Boolean> done) {
        this.input = input;
        this.data = data;
        this.cacheprocessors = cacheprocessors;
        this.done = done;
    }
    public void run() {
        while (!done.value) {
            Packet pkt;
            try {
                pkt = input.deq();
            } catch (Exception e) {
                continue;
            }
            if (pkt == null) {
                continue;
            }
            if (pkt.type == Packet.MessageType.ConfigPacket) {
                // Config packets are processed manually
                this.data.processConfig(pkt.config);
                packetsProcessed++;
                continue;
            }
            if (pkt.header == null) {
                System.out.println("Nah man");
            }
            // Send to the cache processor. Do this by the hash of the packet.
            int tableIndex = this.data.cache.hashMod(pkt.header.source,pkt.header.dest,this.data.cache.cache.length());
            // System.out.println("tableIdx is "+ tableIndex);
            int realIdx = tableIndex / (this.data.cache.cache.length() / cacheprocessors.length);
            // System.out.println("Real index is "+ realIdx);

            boolean s = false;
            while (!s && !done.value){
                try {
                    cacheprocessors[realIdx].enq(pkt);
                    cachesenq++;
                    s = true;
                } catch (FullException e) {
                }
            }
        }
    }

    
}
