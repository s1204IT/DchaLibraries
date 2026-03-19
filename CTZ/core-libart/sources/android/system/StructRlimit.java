package android.system;

import libcore.util.Objects;

public final class StructRlimit {
    public final long rlim_cur;
    public final long rlim_max;

    public StructRlimit(long j, long j2) {
        this.rlim_cur = j;
        this.rlim_max = j2;
    }

    public String toString() {
        return Objects.toString(this);
    }
}
