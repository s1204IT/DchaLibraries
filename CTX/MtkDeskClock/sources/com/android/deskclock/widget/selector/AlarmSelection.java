package com.android.deskclock.widget.selector;

import com.android.deskclock.provider.Alarm;

public class AlarmSelection {
    private final Alarm mAlarm;
    private final String mLabel;

    public AlarmSelection(String str, Alarm alarm) {
        this.mLabel = str;
        this.mAlarm = alarm;
    }

    public String getLabel() {
        return this.mLabel;
    }

    public Alarm getAlarm() {
        return this.mAlarm;
    }
}
