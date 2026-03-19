package android.hardware.biometrics.fingerprint.V2_1;

import java.util.ArrayList;

public final class RequestStatus {
    public static final int SYS_EACCES = -13;
    public static final int SYS_EAGAIN = -11;
    public static final int SYS_EBUSY = -16;
    public static final int SYS_EFAULT = -14;
    public static final int SYS_EINTR = -4;
    public static final int SYS_EINVAL = -22;
    public static final int SYS_EIO = -5;
    public static final int SYS_ENOENT = -2;
    public static final int SYS_ENOMEM = -12;
    public static final int SYS_ENOSPC = -28;
    public static final int SYS_ETIMEDOUT = -110;
    public static final int SYS_OK = 0;
    public static final int SYS_UNKNOWN = 1;

    public static final String toString(int i) {
        if (i == 1) {
            return "SYS_UNKNOWN";
        }
        if (i == 0) {
            return "SYS_OK";
        }
        if (i == -2) {
            return "SYS_ENOENT";
        }
        if (i == -4) {
            return "SYS_EINTR";
        }
        if (i == -5) {
            return "SYS_EIO";
        }
        if (i == -11) {
            return "SYS_EAGAIN";
        }
        if (i == -12) {
            return "SYS_ENOMEM";
        }
        if (i == -13) {
            return "SYS_EACCES";
        }
        if (i == -14) {
            return "SYS_EFAULT";
        }
        if (i == -16) {
            return "SYS_EBUSY";
        }
        if (i == -22) {
            return "SYS_EINVAL";
        }
        if (i == -28) {
            return "SYS_ENOSPC";
        }
        if (i == -110) {
            return "SYS_ETIMEDOUT";
        }
        return "0x" + Integer.toHexString(i);
    }

    public static final String dumpBitfield(int i) {
        ArrayList arrayList = new ArrayList();
        int i2 = 1;
        if ((i & 1) == 1) {
            arrayList.add("SYS_UNKNOWN");
        } else {
            i2 = 0;
        }
        arrayList.add("SYS_OK");
        if ((i & (-2)) == -2) {
            arrayList.add("SYS_ENOENT");
            i2 |= -2;
        }
        if ((i & (-4)) == -4) {
            arrayList.add("SYS_EINTR");
            i2 |= -4;
        }
        if ((i & (-5)) == -5) {
            arrayList.add("SYS_EIO");
            i2 |= -5;
        }
        if ((i & (-11)) == -11) {
            arrayList.add("SYS_EAGAIN");
            i2 |= -11;
        }
        if ((i & (-12)) == -12) {
            arrayList.add("SYS_ENOMEM");
            i2 |= -12;
        }
        if ((i & (-13)) == -13) {
            arrayList.add("SYS_EACCES");
            i2 |= -13;
        }
        if ((i & (-14)) == -14) {
            arrayList.add("SYS_EFAULT");
            i2 |= -14;
        }
        if ((i & (-16)) == -16) {
            arrayList.add("SYS_EBUSY");
            i2 |= -16;
        }
        if ((i & (-22)) == -22) {
            arrayList.add("SYS_EINVAL");
            i2 |= -22;
        }
        if ((i & (-28)) == -28) {
            arrayList.add("SYS_ENOSPC");
            i2 |= -28;
        }
        if ((i & SYS_ETIMEDOUT) == -110) {
            arrayList.add("SYS_ETIMEDOUT");
            i2 |= SYS_ETIMEDOUT;
        }
        if (i != i2) {
            arrayList.add("0x" + Integer.toHexString(i & (~i2)));
        }
        return String.join(" | ", arrayList);
    }
}
