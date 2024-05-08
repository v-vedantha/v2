public class CacheProcessor {
    AllData data;
    public CacheProcessor(AllData data) {
        this.data = data;
    }

    // to process a cache lookup just try it out. If it works add it to the histogram
    public boolean processCacheLookup(Header header, Boolean[] isAllowed) {
        Boolean[] isInCache = {false};
        CacheEntry res = data.cache.lookup(header.source, header.dest, isInCache);
        if (isInCache[0]) {
            // System.out.println("Cache find");
            if (res.ts >= data.ts.get(header.source % 256) && res.ts >= data.ts.get(header.dest % 256)) {
                // System.out.println("Cache hit");
                if (res.isAllowed == true) {
                    isAllowed[0] = true;
                } else {
                    isAllowed[0] = false;
                }
                return true;
            }
        }
        return false;
    }
}
