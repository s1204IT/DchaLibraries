package com.android.bluetooth.util;

import com.android.bluetooth.DeviceWorkArounds;
import java.util.ArrayList;
import java.util.List;

public class Interop {
    public static final int INTEROP_MAP_ASCIIONLY = 1;
    private static List<Entry> sEntries = null;

    private static class Entry {
        public String address;
        public int workaround_id;

        Entry(int i, String str) {
            this.workaround_id = i;
            this.address = str;
        }
    }

    private static void lazyInitInteropDatabase() {
        if (sEntries != null) {
            return;
        }
        sEntries = new ArrayList();
        sEntries.add(new Entry(1, DeviceWorkArounds.MERCEDES_BENZ_CARKIT));
    }

    public static boolean matchByAddress(int i, String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        lazyInitInteropDatabase();
        for (Entry entry : sEntries) {
            if (entry.workaround_id == i && entry.address.startsWith(str.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
