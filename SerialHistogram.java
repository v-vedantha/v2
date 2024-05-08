public class SerialHistogram {
    Integer[] histogram = new Integer[256];
    public SerialHistogram() {

    }
    public void add(long fingerprint) {
        int index = (int) (fingerprint & 0xFF);
        this.histogram[index] = this.histogram[index] + 1;
    } 
}
