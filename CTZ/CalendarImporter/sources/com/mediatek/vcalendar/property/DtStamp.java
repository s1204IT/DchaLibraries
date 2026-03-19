package com.mediatek.vcalendar.property;

import com.mediatek.vcalendar.utils.LogUtil;

public class DtStamp extends Property {
    private static final String TAG = "DtStamp";

    public DtStamp(String str) {
        super(Property.DTSTAMP, str);
        LogUtil.d(TAG, "Constructor: DTSTAMP created.");
    }
}
