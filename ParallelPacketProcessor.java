import java.util.HashSet;
import java.util.Hashtable;
import java.util.concurrent.locks.ReentrantLock;

public class ParallelPacketProcessor extends PacketProcessor {
    PacketGenerator gen;    
    SerialHistogram hist;

    // Persona non grata
    Hashtable<Integer, Boolean> PNG;

    // Map from dest -> source packets that can send to here
    Hashtable<Integer, HashSet<Integer>> R;
    // Make a lock
    ReentrantLock lock = new ReentrantLock();
    ParallelPacketProcessor(PacketGenerator gen) {
        this.gen = gen;
        this.PNG = new Hashtable<Integer, Boolean>();
        this.R = new Hashtable<Integer, HashSet<Integer>>();
        this.hist = new SerialHistogram();
    }

    public void processConfig(Config config) {
        this.lock.lock();
        try {
        // Add to the persona non grata list
        this.PNG.put(config.address, config.personaNonGrata);

        // Add to the range list
        for (int i = config.addressBegin; i < config.addressEnd; i++) {
            HashSet<Integer> range = this.R.getOrDefault(config.address, new HashSet<Integer>());
            if (config.acceptingRange == true) {
                range.add(i);
            } else {
                range.remove(i);
            }
        }} finally {
            lock.unlock();
        }

    }
    public void processData(Header header, Body body) {
        this.lock.lock();
        try {
        // If your source is a persona non grata, drop the packet
        if (this.PNG.getOrDefault(header.source, false) == true) {
            return;
        }
        // If the source is not in the destination's range, drop the packet
        if (this.R.getOrDefault(header.dest, new HashSet<Integer>()).contains(header.source) == false) {
            return;
        }

        // Process into the histogram
        this.hist.add(Fingerprint.getFingerprint(body.iterations, body.seed));

        } finally {
            lock.unlock();
        }
    }
}
