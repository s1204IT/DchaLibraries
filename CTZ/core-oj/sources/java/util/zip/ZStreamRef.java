package java.util.zip;

class ZStreamRef {
    private volatile long address;

    ZStreamRef(long j) {
        this.address = j;
    }

    long address() {
        return this.address;
    }

    void clear() {
        this.address = 0L;
    }
}
