package com.mediatek.calendarimporter.utils;

import com.google.android.collect.Maps;
import java.util.HashMap;

public class InjectedServices {
    private HashMap<String, Object> mSystemServices;

    public void setSystemService(String str, Object obj) {
        if (this.mSystemServices == null) {
            this.mSystemServices = Maps.newHashMap();
        }
        this.mSystemServices.put(str, obj);
    }

    public Object getSystemService(String str) {
        if (this.mSystemServices != null) {
            return this.mSystemServices.get(str);
        }
        return null;
    }
}
