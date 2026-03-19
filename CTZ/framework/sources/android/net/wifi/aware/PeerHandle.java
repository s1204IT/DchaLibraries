package android.net.wifi.aware;

public class PeerHandle {
    public int peerId;

    public PeerHandle(int i) {
        this.peerId = i;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return (obj instanceof PeerHandle) && this.peerId == ((PeerHandle) obj).peerId;
    }

    public int hashCode() {
        return this.peerId;
    }
}
