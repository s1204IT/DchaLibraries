package com.mediatek.vcalendar.database;

import android.content.Context;
import android.database.Cursor;
import android.provider.CalendarContract;
import com.mediatek.vcalendar.SingleComponentCursorInfo;
import com.mediatek.vcalendar.utils.CursorUtil;
import com.mediatek.vcalendar.utils.LogUtil;

public class VEventInfoHelper extends ComponentInfoHelper {
    private static final String TAG = "VEventInfoHelper";

    protected VEventInfoHelper(Context context, String str) {
        super(context, str);
    }

    @Override
    protected boolean buildComponentInfo(Cursor cursor, SingleComponentCursorInfo singleComponentCursorInfo) {
        LogUtil.i(TAG, "buildComponentInfo()");
        singleComponentCursorInfo.cursor = CursorUtil.copyCurrentRow(cursor);
        singleComponentCursorInfo.componentType = this.mType;
        singleComponentCursorInfo.calendarId = cursor.getLong(cursor.getColumnIndex("calendar_id"));
        singleComponentCursorInfo.calendarName = null;
        try {
            long j = cursor.getLong(cursor.getColumnIndex("_id"));
            if (j == -1) {
                return false;
            }
            Cursor cursorQuery = this.mContentResolver.query(CalendarContract.Reminders.CONTENT_URI, null, "event_id=" + j, null, null);
            if (cursorQuery == null) {
                LogUtil.e(TAG, "buildComponentInfo(): Get the reminders failed.");
                return false;
            }
            singleComponentCursorInfo.remindersCursor = CursorUtil.copyOneCursor(cursorQuery);
            cursorQuery.close();
            Cursor cursorQuery2 = this.mContentResolver.query(CalendarContract.Attendees.CONTENT_URI, null, "event_id=" + j, null, null);
            if (cursorQuery2 == null) {
                LogUtil.e(TAG, "buildComponentInfo(): Get the attendees failed.");
                return false;
            }
            singleComponentCursorInfo.attendeesCursor = CursorUtil.copyOneCursor(cursorQuery2);
            cursorQuery2.close();
            return true;
        } catch (IllegalArgumentException e) {
            LogUtil.e(TAG, "buildComponentInfo():\t" + e.getMessage());
            return false;
        }
    }
}
