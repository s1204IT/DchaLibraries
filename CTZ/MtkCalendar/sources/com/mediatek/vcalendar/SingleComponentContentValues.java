package com.mediatek.vcalendar;

import android.content.ContentValues;
import java.util.LinkedList;

public class SingleComponentContentValues {
    public String componentType;
    public final ContentValues contentValues = new ContentValues();
    public final LinkedList<ContentValues> alarmValuesList = new LinkedList<>();
    public final LinkedList<ContentValues> attendeeValuesList = new LinkedList<>();
}
