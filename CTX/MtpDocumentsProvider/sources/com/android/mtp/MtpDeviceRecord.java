package com.android.mtp;

class MtpDeviceRecord {
    public final int deviceId;
    public final String deviceKey;
    public final int[] eventsSupported;
    public final String name;
    public final boolean opened;
    public final int[] operationsSupported;
    public final MtpRoot[] roots;

    MtpDeviceRecord(int i, String str, String str2, boolean z, MtpRoot[] mtpRootArr, int[] iArr, int[] iArr2) {
        this.deviceId = i;
        this.name = str;
        this.opened = z;
        this.roots = mtpRootArr;
        this.deviceKey = str2;
        this.operationsSupported = iArr;
        this.eventsSupported = iArr2;
    }

    static boolean isSupported(int[] iArr, int i) {
        if (iArr == null) {
            return false;
        }
        for (int i2 : iArr) {
            if (i2 == i) {
                return true;
            }
        }
        return false;
    }

    static boolean isPartialReadSupported(int[] iArr, long j) {
        if (isSupported(iArr, 38337)) {
            return true;
        }
        return 0 <= j && j <= 4294967295L && isSupported(iArr, 4123);
    }

    static boolean isWritingSupported(int[] iArr) {
        return isSupported(iArr, 4108) && isSupported(iArr, 4109);
    }
}
