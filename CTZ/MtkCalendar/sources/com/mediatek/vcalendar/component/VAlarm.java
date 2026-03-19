package com.mediatek.vcalendar.component;

import android.content.ContentValues;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.property.Property;
import com.mediatek.vcalendar.utils.LogUtil;
import java.util.Iterator;
import java.util.LinkedList;

public class VAlarm extends Component {
    public VAlarm(Component component) {
        super("VALARM", component);
        LogUtil.d("VAlarm", "Constructor: VALARM Component created.");
    }

    @Override
    protected void writeInfoToContentValues(LinkedList<ContentValues> linkedList) throws VCalendarException {
        LogUtil.d("VAlarm", "writeInfoToContentValues()");
        super.writeInfoToContentValues(linkedList);
        if ("VEVENT".equals(getParent().getName())) {
            ContentValues contentValues = new ContentValues();
            Iterator<String> it = getPropertyNames().iterator();
            while (it.hasNext()) {
                Property firstProperty = getFirstProperty(it.next());
                if (firstProperty != null) {
                    firstProperty.writeInfoToContentValues(contentValues);
                }
            }
            linkedList.add(contentValues);
        }
    }

    @Override
    protected void writeInfoToContentValues(ContentValues contentValues) throws VCalendarException {
        LogUtil.d("VAlarm", "writeInfoToContentValues()");
        super.writeInfoToContentValues(contentValues);
        if ("VEVENT".equals(getParent().getName()) && !contentValues.containsKey("hasAlarm")) {
            contentValues.put("hasAlarm", (Integer) 1);
        }
    }
}
