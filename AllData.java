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
    
}
