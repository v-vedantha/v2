public class RProcessor {
    AllData data;
    public RProcessor(AllData data) {
        this.data = data;
    }
    public boolean processPacket(Header header, Integer[] outTs) { 
        // First read the timestamps for the source and destinations
        Integer tsDest = this.data.ts.get(header.dest % 256);
        outTs[0] = tsDest;

        Boolean[] isInQuad = {false};
        SkipList sl = this.data.R.contains_real(header.dest, isInQuad);
        if (isInQuad[0] == false) {
            return false;
        }
        if (sl.contains(new T(header.source)) == false) {
            return false;
        } else {
            return true;
        }
    }
}
