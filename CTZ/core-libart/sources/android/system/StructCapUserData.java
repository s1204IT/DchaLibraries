package android.system;

import libcore.util.Objects;

public final class StructCapUserData {
    public final int effective;
    public final int inheritable;
    public final int permitted;

    public StructCapUserData(int i, int i2, int i3) {
        this.effective = i;
        this.permitted = i2;
        this.inheritable = i3;
    }

    public String toString() {
        return Objects.toString(this);
    }
}
