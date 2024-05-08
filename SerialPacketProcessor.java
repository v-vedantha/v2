import java.util.HashSet;
import java.util.Hashtable;

public class SerialPacketProcessor extends PacketProcessor {
    PacketGenerator gen;    
    SerialHistogram hist;

    // Persona non grata
    Hashtable<Integer, Boolean> PNG;

    // Map from dest -> source packets that can send to here
    Hashtable<Integer, HashSet<Integer>> R;
    SerialPacketProcessor(PacketGenerator gen) {
        this.gen = gen;
        this.PNG = new Hashtable<Integer, Boolean>();
        this.R = new Hashtable<Integer, HashSet<Integer>>();
        this.hist = new SerialHistogram();
    }

    public void processConfig(Config config) {
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
        }

    }
    public void processData(Header header, Body body) {
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

    }
}
