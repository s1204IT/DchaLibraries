package com.android.providers.contacts.util;

import com.android.internal.util.MemInfoReader;

public final class MemoryUtils {
    private static long sTotalMemorySize = -1;

    public static long getTotalMemorySize() {
        if (sTotalMemorySize < 0) {
            MemInfoReader memInfoReader = new MemInfoReader();
            memInfoReader.readMemInfo();
            sTotalMemorySize = memInfoReader.getTotalSize();
        }
        return sTotalMemorySize;
    }
}
