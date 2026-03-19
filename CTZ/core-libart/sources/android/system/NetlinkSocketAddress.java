package android.system;

import java.net.SocketAddress;
import libcore.util.Objects;

public final class NetlinkSocketAddress extends SocketAddress {
    private final int nlGroupsMask;
    private final int nlPortId;

    public NetlinkSocketAddress() {
        this(0, 0);
    }

    public NetlinkSocketAddress(int i) {
        this(i, 0);
    }

    public NetlinkSocketAddress(int i, int i2) {
        this.nlPortId = i;
        this.nlGroupsMask = i2;
    }

    public int getPortId() {
        return this.nlPortId;
    }

    public int getGroupsMask() {
        return this.nlGroupsMask;
    }

    public String toString() {
        return Objects.toString(this);
    }
}
