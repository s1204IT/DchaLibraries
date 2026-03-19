package com.mediatek.vcalendar.parameter;

import android.content.ContentValues;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.component.Component;
import com.mediatek.vcalendar.utils.LogUtil;

public class Parameter {
    protected Component mComponent;
    protected String mName;
    protected String mValue;

    public Parameter(String str, String str2) {
        this.mName = str;
        this.mValue = str2;
    }

    public String getName() {
        return this.mName;
    }

    public String getValue() {
        return this.mValue;
    }

    public void setValue(String str) {
        this.mValue = str;
    }

    public void setComponent(Component component) {
        this.mComponent = component;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    public void toString(StringBuilder sb) {
        sb.append(this.mName);
        sb.append("=");
        sb.append(this.mValue);
    }

    public void writeInfoToContentValues(ContentValues contentValues) throws VCalendarException {
        if (contentValues == null) {
            LogUtil.e("Parameter", "toAttendeesContentValue: the argument ContentValue must not be null.");
            throw new VCalendarException();
        }
    }
}
