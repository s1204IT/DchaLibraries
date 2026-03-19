package com.mediatek.vcalendar.parameter;

import com.mediatek.vcalendar.utils.LogUtil;

public class Value extends Parameter {
    public Value(String str) {
        super("VALUE", str);
        LogUtil.d("Value", "Constructor: value parameter created");
    }
}
