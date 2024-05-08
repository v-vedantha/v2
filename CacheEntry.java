public class CacheEntry {
    public Integer source;
    public Integer dest;
    public Boolean isAllowed;
    public Integer ts;
    CacheEntry(Integer source, Integer dest, Boolean isAllowed, Integer ts) {
        this.source = source;
        this.dest = dest;
        this.isAllowed = isAllowed;
        this.ts = ts;
    }
}
