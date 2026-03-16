package com.android.providers.contacts.util;

import com.android.internal.util.MemInfoReader;

public final class MemoryUtils {
    private static long sTotalMemorySize = -1;

    public static long getTotalMemorySize() {
        if (sTotalMemorySize < 0) {
            MemInfoReader reader = new MemInfoReader();
            reader.readMemInfo();
            sTotalMemorySize = reader.getTotalSize();
        }
        return sTotalMemorySize;
    }
}
