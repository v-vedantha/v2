import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;

public class Cache {

    // The cache will consist of an extremely standard hash table.
    // We don't want to take out locks on reading this, so we'll be a little fancy with the design.
    // We will take a lock out for each source and destination address
    private class CacheEntry {
        Integer source;
        Integer dest;
        Boolean isAllowed;
        CacheEntry(Integer source, Integer dest, Boolean isAllowed) {
            this.source = source;
            this.dest = dest;
            this.isAllowed = isAllowed;
        }
    }

    public volatile AtomicReferenceArray<CacheEntry> cache;


    public Cache(int l) {
        cache = new AtomicReferenceArray<CacheEntry>(l);
    }

    public void invalidateSource(Integer source){
        // So first invalidate everything that corresponds to this source without locking
        for (int i = 0; i < cache.length(); i++) {
            CacheEntry e = cache.get(i);
            if (e != null && e.source == source) {
                cache.set(i, null);
            }
        }
    }

    public void invalidateRange(Integer sourceBegin, Integer sourceEnd, Integer dest){
        for (int i = 0; i < cache.length(); i++) {
            CacheEntry e = cache.get(i);
            if (e != null && e.source >= sourceBegin && e.source < sourceEnd && e.dest == dest) {
                cache.set(i, null);
            }
        }
    }

    public boolean validateSource(Integer source) {
        for (int i = 0; i < cache.length(); i++) {
            CacheEntry e = cache.get(i);
            if (e != null && e.source == source) {
                return false;
            }
        }
        return true;
    }
    public boolean validateElem(Integer source, Integer dest){
        Integer index = hashMod(source, dest, cache.length());
        CacheEntry e = cache.get(index);
        if (e != null && e.source == source && e.dest == dest) {
            return false;
        }
        return true;
    }

    public boolean validateRange(int sourceBegin, int sourceEnd, int dest) {
        for (int src = sourceBegin; src < sourceEnd; src++) {
            boolean res = validateElem(src, dest);
            if (!res) {
                return false;
            }
        }
        return true;
    }

    public Integer hash(Integer source, Integer dest) {
        return source * 31 + dest;
    }
    public Integer hashMod(Integer source, Integer dest, Integer length) {
        return Math.floorMod(hash(source, dest), length);
    }
    public Integer hashMod(Integer source, Integer length) {
        return Math.floorMod(source, length);
    }

    public boolean lookup(Integer source, Integer dest, Boolean[] isCached) {
        // Just see if we have it in the cache
        // Even if you look up during a resize, its no big deal
        int index = hashMod(source, dest, cache.length());
        CacheEntry e = cache.get(index);

        if (e != null && e.source == source && e.dest == dest) {
            isCached[0] = true;
            return e.isAllowed;
        }
        isCached[0] = false;
        return false;
    }

    public void addElement(Integer source, Integer dest, Boolean isAllowed) {
        int index = hashMod(source, dest, cache.length());
        // If we displace something it's not the end of the world. The element just got displaced
        cache.set(index, new CacheEntry(source, dest, isAllowed));
    }
    
}
