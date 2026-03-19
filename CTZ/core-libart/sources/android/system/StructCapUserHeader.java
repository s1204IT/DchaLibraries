package android.system;

import libcore.util.Objects;

public final class StructCapUserHeader {
    public final int pid;
    public int version;

    public StructCapUserHeader(int i, int i2) {
        this.version = i;
        this.pid = i2;
    }

    public String toString() {
        return Objects.toString(this);
    }
}
