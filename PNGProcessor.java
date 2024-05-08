public class PNGProcessor {
    AllData data;
    public PNGProcessor(AllData data) {
        this.data = data;
    }
    public boolean processCacheLookup(Header header, Integer[] outTs) { 
        // First read the timestamps for the source and destinations
        Integer tsSource = this.data.ts.get(header.source % 256);
        outTs[0] = tsSource;
        // Integer tsDest = this.data.ts.get(header.dest % 256);

        // Check if the source is in the persona non grata list
        if (this.data.PNG.contains(header.source)) {
            return false;
        } else {
            return true;
        }
    }
}
