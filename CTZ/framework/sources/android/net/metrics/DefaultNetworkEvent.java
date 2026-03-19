package android.net.metrics;

import android.net.NetworkCapabilities;
import android.security.keystore.KeyProperties;
import com.android.internal.util.BitUtils;
import java.util.StringJoiner;

public class DefaultNetworkEvent {
    public final long creationTimeMs;
    public long durationMs;
    public int finalScore;
    public int initialScore;
    public boolean ipv4;
    public boolean ipv6;
    public int netId = 0;
    public int previousTransports;
    public int transports;
    public long validatedMs;

    public DefaultNetworkEvent(long j) {
        this.creationTimeMs = j;
    }

    public void updateDuration(long j) {
        this.durationMs = j - this.creationTimeMs;
    }

    public String toString() {
        StringJoiner stringJoiner = new StringJoiner(", ", "DefaultNetworkEvent(", ")");
        stringJoiner.add("netId=" + this.netId);
        int[] iArrUnpackBits = BitUtils.unpackBits((long) this.transports);
        int length = iArrUnpackBits.length;
        for (int i = 0; i < length; i++) {
            stringJoiner.add(NetworkCapabilities.transportNameOf(iArrUnpackBits[i]));
        }
        stringJoiner.add("ip=" + ipSupport());
        if (this.initialScore > 0) {
            stringJoiner.add("initial_score=" + this.initialScore);
        }
        if (this.finalScore > 0) {
            stringJoiner.add("final_score=" + this.finalScore);
        }
        stringJoiner.add(String.format("duration=%.0fs", Double.valueOf(this.durationMs / 1000.0d)));
        stringJoiner.add(String.format("validation=%04.1f%%", Double.valueOf((this.validatedMs * 100.0d) / this.durationMs)));
        return stringJoiner.toString();
    }

    private String ipSupport() {
        if (this.ipv4 && this.ipv6) {
            return "IPv4v6";
        }
        if (this.ipv6) {
            return "IPv6";
        }
        if (this.ipv4) {
            return "IPv4";
        }
        return KeyProperties.DIGEST_NONE;
    }
}
