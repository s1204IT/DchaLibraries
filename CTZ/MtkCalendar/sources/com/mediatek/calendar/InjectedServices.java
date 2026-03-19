package com.mediatek.calendar;

import java.util.HashMap;

public class InjectedServices {
    private HashMap<String, Object> mSystemServices;

    public Object getSystemService(String str) {
        if (this.mSystemServices != null) {
            return this.mSystemServices.get(str);
        }
        return null;
    }
}
