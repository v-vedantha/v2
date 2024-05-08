import java.util.concurrent.atomic.AtomicInteger;

public class ParallelHistogram {
    AtomicInteger[] histogram = new AtomicInteger[256];
    public ParallelHistogram() {
        for (int i = 0; i < 256; i++) {
            this.histogram[i] = new AtomicInteger(0);
        }

    }
    public void add(long fingerprint) {
        int index = (int) (fingerprint & 0xFF);
        this.histogram[index].getAndIncrement();
    } 
}
