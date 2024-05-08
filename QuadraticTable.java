import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class QuadraticTable {
    // A thing consists of both a value and an amount to jump
    class Entry {
        Integer value;
        SkipList skiplist;
        int jump;
        Entry(Integer value, int jump, SkipList skiplist) {
            this.value = value;
            this.jump = jump;
            this.skiplist = skiplist;
        }
    }

    public  AtomicReferenceArray<Entry> table;
    AtomicInteger size;
    private ReadWriteLock[] locks;
    private ReadWriteLock[] baselocks;
    private AtomicBoolean resizing = new AtomicBoolean(false);

    public QuadraticTable(int capacity) {
        size = new AtomicInteger(0);

        table = new AtomicReferenceArray<Entry>(capacity);
        locks = new ReadWriteLock[capacity];
        baselocks = new ReadWriteLock[capacity];
        for (int i = 0; i < capacity; i++) {
            locks[i] = new ReentrantReadWriteLock();
            baselocks[i] = new ReentrantReadWriteLock();
        }
        for (int i = 0; i < capacity; i++) {
            table.set(i, new Entry(null, 0, new SkipList()));
        }
    }
    public QuadraticTable(int capacity, int tableSize) {
        size = new AtomicInteger(0);

        table = new AtomicReferenceArray<>(tableSize);
        locks = new ReadWriteLock[capacity];
        baselocks = new ReadWriteLock[capacity];
        for (int i = 0; i < capacity; i++) {
            locks[i] = new ReentrantReadWriteLock();
            baselocks[i] = new ReentrantReadWriteLock();
        }
        for (int i = 0; i < tableSize; i++) {
            table.set(i, new Entry(null, 0, new SkipList()));
        }
    }

    public void acquire(Integer x, int offset) {
        locks[Math.floorMod(x.hashCode() + offset * offset , locks.length)].readLock().lock();
    }

    public void release(Integer x, int offset) {
        locks[Math.floorMod(x.hashCode() + offset * offset , locks.length)].readLock().unlock();
    }
    public void baseAcquire(Integer x) {
        baselocks[Math.floorMod(x.hashCode() , baselocks.length)].readLock().lock();
    }
    public void baseRelease(Integer x) {
        baselocks[Math.floorMod(x.hashCode() , baselocks.length)].readLock().unlock();
    }

    public void acquireW(Integer x, int offset) {
        locks[Math.floorMod(x.hashCode() + offset * offset , locks.length)].writeLock().lock();
    }

    public void releaseW(Integer x, int offset) {
        locks[Math.floorMod(x.hashCode() + offset * offset, locks.length)].writeLock().unlock();
    }
    public void baseAcquireW(Integer x) {
        baselocks[Math.floorMod(x.hashCode() , baselocks.length)].writeLock().lock();
    }
    public void baseReleaseW(Integer x) {
        baselocks[Math.floorMod(x.hashCode() , baselocks.length)].writeLock().unlock();
    }
    public SkipList contains_real(Integer x, Boolean[] result) {
        // System.out.println("Startinga a contains");
        AtomicReferenceArray<Entry> table2 = this.table;
        int myBucket = Math.floorMod(x.hashCode() , table2.length());
        int i = 0;
        // acquire(x, i);
        while (i <= table2.get(myBucket).jump) {
            // System.out.println("getting" + Math.floorMod(myBucket + i*i, table2.length()));
            Entry e = table2.get(Math.floorMod(myBucket + i*i, table2.length()));
            if (e.value != null && e.value.hashCode() == x.hashCode()) {
                // release(x, i);
                // System.out.println("end a contains");
                result[0] = true;
                return e.skiplist;
            }
            // release(x, i);
            i++;
            // acquire(x, i);
        }
        // release(x, i);
        // System.out.println("end a contains");
        result[0] = false;
        return null;
    }

    public boolean contains(Integer x, boolean acq_base) {
        AtomicReferenceArray<Entry> table2 = this.table;
        // System.out.println("Startinga a contains");
        int myBucket = Math.floorMod(x.hashCode() , table.length());
        int i = 0;
        if (acq_base) {
            baseAcquire(x);
        }

        // acquire(x, i);
        while (i <= table2.get(myBucket).jump) {
            // System.out.println("getting" + Math.floorMod(myBucket + i*i, table.length()));
            if (table2.get(Math.floorMod(myBucket + i*i, table2.length())).value != null && table2.get(Math.floorMod(myBucket + i*i, table2.length())).value.hashCode() == x.hashCode()) {
                // release(x, i);
                if (acq_base) {
                    baseRelease(x);
                }
                // System.out.println("end a contains");
                return true;
            }
            // release(x, i);
            i++;
            // acquire(x, i);
        }
        // release(x, i);
        if (acq_base) {
            baseRelease(x);
        }
        // System.out.println("end a contains");
        return false;
    }
    public boolean contains(Integer x) {
        return contains(x, true);
    }
    public void resize() {
        // System.out.println("Starting a resaize");
        if (!resizing.compareAndSet(false, true)) {
            return;
        }
        for (ReadWriteLock baseLock : baselocks) {
            baseLock.writeLock().lock();
        }
        // Remake the table
        QuadraticTable newIntegerable = new QuadraticTable(locks.length, table.length() * 2);
        for (int i = 0; i < table.length(); i++) {
            Entry entry = table.get(i);
            if (entry.value != null) {
                newIntegerable.add(entry.value, entry.skiplist);
            }
        }
        this.table = newIntegerable.table;

        for (ReadWriteLock baseLock : baselocks) {
            baseLock.writeLock().unlock();
        }
        // System.out.println("Finishing a resize");
        resizing.set(false);
    }
    public boolean add(Integer x, SkipList s) {
        // System.out.println("Starting an add");
        baseAcquireW(x);
        if (contains(x, false)) {
            baseReleaseW(x);
            // System.out.println("Finishing an add");
            return false;
        }
        int myBucket = Math.floorMod(x.hashCode() , table.length());
        boolean succeed = false;
        int i = 0;
        while (succeed == false) {
            int idx = Math.floorMod((myBucket + i*i) , table.length());
            while (table.get(idx).value != null) {
                i++;
                idx = Math.floorMod((myBucket + i*i) , table.length());
            }
            Entry newVal = table.get(idx);
            Entry newVal2 = new Entry(x, i, s);
            newVal2.value = x;
            succeed = table.compareAndSet(idx,newVal, newVal2);
            // succeed = true;
            // newVal.skiplist = s;
        }
        boolean succeed2 = false;
        while (succeed2 == false) {
            Entry currVal = table.get(Math.floorMod((myBucket) , table.length()));
            Entry currVal2 = new Entry(currVal.value, currVal.jump, currVal.skiplist);
            currVal2.jump = i > currVal.jump ? i : currVal.jump;
            if (i <= currVal.jump) {break;}
            succeed2 = table.compareAndSet(Math.floorMod((myBucket) , table.length()),currVal, currVal2);
        }
        // releaseW(x, i);
        baseReleaseW(x);
        size.getAndIncrement();
        if (size.get() * 4> table.length()) {
            resize();
        }
        // System.out.println("Finishing an add");
        return true;
    }

    public boolean remove(Integer x) {
        // System.out.println("Starting a remove");
        baseAcquireW(x);
        if (!contains(x, false)) {
            baseReleaseW(x);
            // System.out.println("ending a remove");
            return false;
        }
        int i = 0;
        int myBucket = Math.floorMod(x.hashCode() , table.length());
        while (table.get(Math.floorMod((myBucket + i * i), table.length())).value == null
                || table.get(Math.floorMod((myBucket + i * i), table.length())).value.hashCode() != x.hashCode()) {
            i++;
            if (i > table.get(myBucket).jump) {
                // Integerhis should never happen
                throw new RuntimeException("cuh");
                // baseRelease(x);
                // return false;
            }
        }
        // We found the value
        // Integero remove it we can just set it to null
        Entry newVal = table.get(Math.floorMod((myBucket + i*i) , table.length()));
        // Entry newVal = table.get(Math.floorMod((myBucket + i*i) , table.length()));
        while ( newVal.value.hashCode() == x.hashCode()) {
            Entry newVal2 = new Entry(null, newVal.jump, new SkipList());
            boolean res = table.compareAndSet(Math.floorMod((myBucket + i*i) , table.length()), newVal, newVal2);
            if (res) {
                size.getAndDecrement();
                break;
            }
            newVal = table.get(Math.floorMod((myBucket + i*i) , table.length()));
        }
        // newVal.value = null;
        // newVal.skiplist = new SkipList();

        // table.set(Math.floorMod((myBucket + i*i) , table.length()), newVal);
        baseReleaseW(x);
        // System.out.println("ending a remove");
        return true;
    }
    public void printLocks() {
        for (int i = 0; i < locks.length; i++) {
            boolean readResult = locks[i].readLock().tryLock();
            if (readResult) {
                locks[i].readLock().unlock();
            }
            boolean writeResult = locks[i].writeLock().tryLock();
            if (writeResult) {
                locks[i].writeLock().unlock();
            }
            // System.out.println(locks[i].readLock() + ""+ locks[i].writeLock() + "" + "" + i + " read: " + readResult + " write: " + writeResult);
        }
    }

}
