package com.mediatek.vcalendar.property;

import com.mediatek.vcalendar.utils.LogUtil;

public class DtStamp extends Property {
    public DtStamp(String str) {
        super("DTSTAMP", str);
        LogUtil.d("DtStamp", "Constructor: DTSTAMP created.");
    }
}
