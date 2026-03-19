package com.android.server.wifi;

import android.os.SystemProperties;

class SystemPropertyService implements PropertyService {
    SystemPropertyService() {
    }

    @Override
    public String get(String str, String str2) {
        return SystemProperties.get(str, str2);
    }

    @Override
    public void set(String str, String str2) {
        SystemProperties.set(str, str2);
    }

    @Override
    public boolean getBoolean(String str, boolean z) {
        return SystemProperties.getBoolean(str, z);
    }

    @Override
    public String getString(String str, String str2) {
        return SystemProperties.get(str, str2);
    }
}
