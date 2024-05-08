import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;

public class Cache {

    // The cache will consist of an extremely standard hash table.
    // We don't want to take out locks on reading this, so we'll be a little fancy with the design.
    // We will take a lock out for each source and destination address

    public volatile AtomicReferenceArray<CacheEntry> cache;


    public Cache(int l) {
        cache = new AtomicReferenceArray<CacheEntry>(l);
    }

    public Integer hash(Integer source, Integer dest) {
        return source * 31 + dest;
    }
    public Integer hashMod(Integer source, Integer dest, Integer length) {
        return Math.floorMod(hash(source, dest), length);
    }

    public CacheEntry lookup(Integer source, Integer dest, Boolean[] isCached) {
        // Just see if we have it in the cache
        // Even if you look up during a resize, its no big deal
        int index = hashMod(source, dest, cache.length());
        CacheEntry e = cache.get(index);

        // if (e != null) {
        //     System.out.println("Currently seeing " + e.source + " " + e.dest +  " for " + source + " " + dest + " " + index + " " + cache.length());
        // }
        if (e != null && e.source == source && e.dest == dest) {
            isCached[0] = true;
            return e;
        }
        isCached[0] = false;
        return null;
    }

    public void addElement(Integer source, Integer dest, Boolean isAllowed, Integer ts) {
        int index = hashMod(source, dest, cache.length());
        // If we displace something it's not the end of the world. The element just got displaced
        cache.set(index, new CacheEntry(source, dest, isAllowed, ts));
    }
    
}
