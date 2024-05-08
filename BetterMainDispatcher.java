public class BetterMainDispatcher implements Runnable {
    
    LamportQ[] dispatchToMe;
    PacketGenerator source;
    PaddedPrimitiveNonVolatile<Boolean> done;

    public BetterMainDispatcher(LamportQ[] dispatchToMe, PacketGenerator source, PaddedPrimitiveNonVolatile<Boolean> done) {
        this.dispatchToMe = dispatchToMe;
        this.source = source;
        this.done = done;

    }

    public void run() {
        int i = 0;
        while (!done.value) {
            i ++;
            Packet pkt = source.getPacket();
            boolean succ = false;
            while (!succ && !done.value) {
                try {
                    dispatchToMe[i % dispatchToMe.length].enq(pkt);
                    succ = true;
                } catch (FullException e) {
                }
            }
        }
    }
}
