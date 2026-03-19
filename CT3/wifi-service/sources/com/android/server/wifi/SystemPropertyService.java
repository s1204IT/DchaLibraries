package com.android.server.wifi;

import android.os.SystemProperties;

class SystemPropertyService implements PropertyService {
    SystemPropertyService() {
    }

    @Override
    public String get(String key, String defaultValue) {
        return SystemProperties.get(key, defaultValue);
    }

    @Override
    public void set(String key, String val) {
        SystemProperties.set(key, val);
    }
}
