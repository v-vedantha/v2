public class PNGWorkers implements Runnable {
    LamportQ input;
    LamportQ headerI;
    PaddedPrimitiveNonVolatile<Boolean> done;
    AllData data;
    public int packetsProcessed = 0;
    PNGProcessor p;
    // If I am the final person to process the packet, then we will add to the histogram, or reprocess if the timstamps do not match
    public PNGWorkers(LamportQ input, PaddedPrimitiveNonVolatile<Boolean> done, AllData data, LamportQ headerI) {
        this.input = input;
        this.done = done;
        this.headerI = headerI;
        this.data = data;
        this.p = new PNGProcessor(data);
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
            // Send to the png processor
            Integer[]  ts = {0};
            boolean success = p.processPacket(pkt.header, ts);
            pkt.sourcetsPNG.set(ts[0]);
            pkt.PNGallowed.set(success);

            // If I am the final person, do the duties w.r.t either enq or histogram
            if (pkt.step.getAndIncrement() == 1) {
                // If both ts values are up-to-date w.r.t the ts of the last update of either element, then we abide by the result of both R and PNG
                int maxts = Math.max(this.data.ts.get(pkt.header.source % 256), this.data.ts.get(pkt.header.dest % 256));
                int mytsS = Math.min(pkt.sourcetsPNG.get(), pkt.destTsR.get());
                int mytsSMax = Math.max(pkt.sourcetsPNG.get(), pkt.destTsR.get());
                if (mytsS >= maxts) {
                    // All good
                    boolean result = success && pkt.Rallowed.get();
                    this.data.cache.addElement(pkt.header.source, pkt.header.dest, result, mytsSMax);
                    packetsProcessed++;
                    if (result) {
                        // Add to histogram
                        this.data.hist.add(Fingerprint.getFingerprint(pkt.body.iterations, pkt.body.seed));
                    } else {
                        // Do nothing
                    }
                } else 
                {
                    // Reprocess
                    pkt.step.set(0);
                    boolean s = false;
                    while (!s && !done.value){
                        try {
                            headerI.enq(pkt);
                            s = true;
                        } catch (FullException e) {
                        }

                    }
                }
            } else {
                // Don't do anything, more processing TBD
            }
        }
    }
}
