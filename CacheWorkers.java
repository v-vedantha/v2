public class CacheWorkers implements Runnable {
    LamportQ input;
    PaddedPrimitiveNonVolatile<Boolean> done;
    AllData data;
    CacheProcessor p;
    LamportQ[] pngs;
    LamportQ[] rs;
    public int packetsProcessed = 0;
    public CacheWorkers(LamportQ input, PaddedPrimitiveNonVolatile<Boolean> done, AllData data, LamportQ[] pngs, LamportQ[] rs) {
        this.input = input;
        this.done = done;
        this.data = data;
        this.p = new CacheProcessor(data);
        this.pngs = pngs;
        this.rs = rs;
    }    

    public void run () {

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
            Boolean[]  isAllowed = {false};
            boolean success = p.processCacheLookup(pkt.header, isAllowed);
            if (success) {
                    packetsProcessed++;
                if (isAllowed[0]) {
                    // Add to histogram
                    this.data.hist.add(Fingerprint.getFingerprint(pkt.body.iterations, pkt.body.seed));
                } else {
                }
            } else {
                int tableLength = this.data.PNG.table.length();

                int tableIdx = Math.floorMod(pkt.header.source, tableLength);
                int idx = tableIdx / (tableLength / this.pngs.length);
                boolean succ = false;
                while (succ == false && !done.value) {
                    try {
                        this.pngs[idx].enq(pkt);
                        succ = true;
                    } catch (FullException e) {
                        // System.out.println("Queue is full");
                    }
                }
                tableLength = this.data.R.table.length();

                tableIdx = Math.floorMod(pkt.header.source, tableLength);
                idx = tableIdx / (tableLength / this.pngs.length);
                succ = false;
                while (succ == false && !done.value) {
                    try {
                        this.rs[idx].enq(pkt);
                        succ = true;
                    } catch (FullException e) {
                        // System.out.println("Queue is full");
                    }
                }
            }
        }
    }
}
