package sun.nio.ch;

public abstract class AbstractPollArrayWrapper {
    static final short EVENT_OFFSET = 4;
    static final short FD_OFFSET = 0;
    static final short REVENT_OFFSET = 6;
    static final short SIZE_POLLFD = 8;
    protected AllocatedNativeObject pollArray;
    protected long pollArrayAddress;
    protected int totalChannels = 0;

    int getEventOps(int i) {
        return this.pollArray.getShort((8 * i) + 4);
    }

    int getReventOps(int i) {
        return this.pollArray.getShort((8 * i) + 6);
    }

    int getDescriptor(int i) {
        return this.pollArray.getInt((8 * i) + 0);
    }

    void putEventOps(int i, int i2) {
        this.pollArray.putShort((8 * i) + 4, (short) i2);
    }

    void putReventOps(int i, int i2) {
        this.pollArray.putShort((8 * i) + 6, (short) i2);
    }

    void putDescriptor(int i, int i2) {
        this.pollArray.putInt((8 * i) + 0, i2);
    }
}
