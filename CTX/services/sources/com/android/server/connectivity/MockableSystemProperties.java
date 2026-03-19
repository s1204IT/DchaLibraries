package com.android.server.connectivity;

import android.os.SystemProperties;

public class MockableSystemProperties {
    public String get(String str) {
        return SystemProperties.get(str);
    }

    public int getInt(String str, int i) {
        return SystemProperties.getInt(str, i);
    }

    public boolean getBoolean(String str, boolean z) {
        return SystemProperties.getBoolean(str, z);
    }

    public void set(String str, String str2) {
        SystemProperties.set(str, str2);
    }
}
