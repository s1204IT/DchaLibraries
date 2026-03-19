package com.mediatek.vcalendar.property;

import android.content.ContentValues;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.utils.LogUtil;

public class Description extends Property {
    private static final String TAG = "Description";

    public Description(String str) {
        super(Property.DESCRIPTION, str);
        LogUtil.d(TAG, "Constructor: Description property created.");
    }

    @Override
    public void writeInfoToContentValues(ContentValues contentValues) throws VCalendarException {
        super.writeInfoToContentValues(contentValues);
        if ("VEVENT".equals(this.mComponent.getName())) {
            contentValues.put("description", this.mValue);
        }
    }
}
