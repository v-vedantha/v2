import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.locks.ReentrantLock;

public class AllData {
    public ParallelHistogram hist;
    public Cache cache;

    // public // Source and destination based locks
    public ReentrantLock[] srcLocks;
    public ReentrantLock[] destLocks;

    // public // Persona non grata
    public QuadraticProbe PNG;

    // public // Map from dest -> source packets that can send to here
    public QuadraticTable R;
    public AtomicIntegerArray ts;
    public AtomicInteger tsS;

    AllData() {
        this.PNG = new QuadraticProbe(8);
        this.R = new QuadraticTable(32);
        this.hist = new ParallelHistogram();
        this.cache = new Cache(1024 * 32);
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

        unlockDest(config.address);
        unlockSource(config.address);
    }
    
}
