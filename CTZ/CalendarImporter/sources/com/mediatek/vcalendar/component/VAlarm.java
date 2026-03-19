package com.mediatek.vcalendar.component;

import android.content.ContentValues;
import android.database.Cursor;
import com.android.common.speech.LoggingEvents;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.property.Action;
import com.mediatek.vcalendar.property.Description;
import com.mediatek.vcalendar.property.Property;
import com.mediatek.vcalendar.property.Trigger;
import com.mediatek.vcalendar.utils.LogUtil;
import com.mediatek.vcalendar.valuetype.DDuration;
import java.util.Iterator;
import java.util.LinkedList;

public class VAlarm extends Component {
    public static final String REMINDER = "Reminder";
    private static final String TAG = "VAlarm";

    public VAlarm(Component component) {
        super("VALARM", component);
        LogUtil.d(TAG, "Constructor: VALARM Component created.");
    }

    @Override
    protected void writeInfoToContentValues(LinkedList<ContentValues> linkedList) throws VCalendarException {
        LogUtil.d(TAG, "writeInfoToContentValues()");
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
        LogUtil.d(TAG, "writeInfoToContentValues()");
        super.writeInfoToContentValues(contentValues);
        if ("VEVENT".equals(getParent().getName()) && !contentValues.containsKey("hasAlarm")) {
            contentValues.put("hasAlarm", (Integer) 1);
        }
    }

    @Override
    protected void compose(Cursor cursor) throws VCalendarException {
        LogUtil.d(TAG, "compose()");
        super.compose(cursor);
        if ("VEVENT".equals(getParent().getName())) {
            String actionString = Action.getActionString(cursor.getInt(cursor.getColumnIndex(LoggingEvents.VoiceIme.EXTRA_START_METHOD)));
            if (actionString != null) {
                addProperty(new Action(actionString));
            }
            String durationString = DDuration.getDurationString((-1) * cursor.getInt(cursor.getColumnIndex("minutes")));
            if (durationString != null) {
                addProperty(new Trigger(durationString));
            }
            addProperty(new Description(REMINDER));
        }
    }
}
