import java.util.HashSet;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;

public class BetterPacketProcessor extends PacketProcessor {
    PacketGenerator gen;    
    ParallelHistogram hist;
    Cache cache;

    // Source and destination based locks
    ReentrantLock[] srcLocks;
    ReentrantLock[] destLocks;

    // Persona non grata
    QuadraticProbe PNG;

    // Map from dest -> source packets that can send to here
    QuadraticTable R;
    AtomicIntegerArray ts;
    AtomicInteger tsS;
    // Make a lock
    ReentrantLock lock = new ReentrantLock();
    BetterPacketProcessor(PacketGenerator gen) {
        this.gen = gen;
        this.PNG = new QuadraticProbe(8);
        this.R = new QuadraticTable(32);
        this.hist = new ParallelHistogram();
        this.cache = new Cache(1024*32);
        this.srcLocks = new ReentrantLock[256];
        this.destLocks = new ReentrantLock[256];
        for (int i = 0; i < 256; i++) {
            this.srcLocks[i] = new ReentrantLock();
            this.destLocks[i] = new ReentrantLock();
        }
        this.ts = new AtomicIntegerArray(256);
        for (int i = 0; i < 256; i++) {
            this.ts.set(i, 0);
        }
        tsS = new AtomicInteger(0);
    }

    void lockSource(Integer source) {
        this.srcLocks[source % 256].lock();
    }
    void unlockSource(Integer source) {
        this.srcLocks[source % 256].unlock();
    }
    void lockDest(Integer dest) {
        this.destLocks[dest % 256].lock();
    }
    void unlockDest(Integer dest) {
        this.destLocks[dest % 256].unlock();
    }

    public void processConfig(Config config) {
        // Step 1 is to process the PNG entry. However, we don't want people to see the update till after I'm done, so let's keep the lock.
        // First empty out the cache
        // this.cache.invalidateSource(config.address);
        // this.cache.invalidateRange(config.addressBegin, config.addressEnd, config.address);
        lockSource(config.address); 
        if (config.personaNonGrata) {
            this.PNG.add(config.address);
        } else {
            this.PNG.remove(config.address);
        }
        ts.set(config.address % 256, tsS.incrementAndGet());


        
        lockDest(config.address);
        // // Also lock the dest
        // // So now we know that only one person can be updating the destination, so we can just use our classic operations
        // // Get the range
        Boolean[] isInQuad = {false};
        SkipList s = this.R.contains_real(config.address, isInQuad);
        if (s == null) {
            this.R.add(config.address, new SkipList());
        }
        s = this.R.contains_real(config.address, isInQuad);
        if (!isInQuad[0]) {
            System.out.println("Fuck");
            throw new RuntimeException("should not happen");
        }

        for (int i = config.addressBegin; i < config.addressEnd; i++) {
            if (config.acceptingRange) {
                s.add(new T(i));
            } else {
                s.remove(new T(i));
            }
        }
        // // Continue emptying it out while validating
        // boolean succeed = false;
        // while (!succeed) {
        //     succeed = true;
        //     this.cache.invalidateSource(config.address);
        //     this.cache.invalidateRange(config.addressBegin, config.addressEnd, config.address);
        //     if (!cache.validateRange(config.addressBegin, config.addressEnd, config.address)) {
        //         continue;
        //     }
        //     if (!cache.validateSource(config.address)) {
        //         continue;
        //     }
        // }

        unlockDest(config.address);
        unlockSource(config.address);
    }
    public void processData(Header header, Body body) {
        Boolean[] isInCache = {false};
        CacheEntry res = cache.lookup(header.source, header.dest, isInCache);
        if (isInCache[0]) {
            System.out.println("Cache find");
            if (res.ts >= ts.get(header.source % 256) && res.ts >= ts.get(header.dest % 256)) {
                System.out.println("Cache hit");
                if (res.isAllowed == true) {
                    this.hist.add(Fingerprint.getFingerprint(body.iterations,body.seed));
                }
                return;
            }
        }
        boolean result = false;
        try {
            lockSource(header.source);
            lockDest(header.dest);
        // If your source is a persona non grata, drop the packet
        if (this.PNG.contains(header.source)) {
            return;
        }
        // If the source is not in the destination's range, drop the packet
        Boolean[] isInQuad = {false};
        SkipList sl = this.R.contains_real(header.dest, isInQuad);
        if (isInQuad[0] == false) {
            return;
        }
        if (sl.contains(new T(header.source)) == false) {
            return;
        }

        // Process into the histogram
        this.hist.add(Fingerprint.getFingerprint(body.iterations, body.seed));
        result = true;
        } finally {
            cache.addElement(header.source, header.dest, result, tsS.get());
            unlockSource(header.source);
            unlockDest(header.dest);
        }
    }
}
