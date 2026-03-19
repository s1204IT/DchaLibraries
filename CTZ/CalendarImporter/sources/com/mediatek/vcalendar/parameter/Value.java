package com.mediatek.vcalendar.parameter;

import com.mediatek.vcalendar.utils.LogUtil;

public class Value extends Parameter {
    public static final String BINARY = "BINARY";
    public static final String BOOLEAN = "BOOLEAN";
    public static final String CAL_ADDRESS = "CAL-ADDRESS";
    public static final String DATE = "DATE";
    public static final String DATE_TIME = "DATE-TIME";
    public static final String DURATION = "DURATION";
    public static final String FLOAT = "FLOAT";
    public static final String INTEGER = "INTEGER";
    public static final String PERIOD = "PERIOD";
    public static final String RECUR = "RECUR";
    private static final String TAG = "Value";
    public static final String TEXT = "TEXT";
    public static final String TIME = "TIME";
    public static final String URI = "URI";
    public static final String UTC_OFFSET = "UTC-OFFSET";

    public Value(String str) {
        super(Parameter.VALUE, str);
        LogUtil.d(TAG, "Constructor: value parameter created");
    }
}
