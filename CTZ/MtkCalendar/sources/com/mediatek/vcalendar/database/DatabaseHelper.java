package com.mediatek.vcalendar.database;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import com.mediatek.vcalendar.SingleComponentContentValues;
import com.mediatek.vcalendar.utils.LogUtil;

public class DatabaseHelper {
    private final ContentResolver mContentResolver;
    private final Context mContext;

    public DatabaseHelper(Context context) {
        this.mContext = context;
        this.mContentResolver = this.mContext.getContentResolver();
    }

    public Uri insert(SingleComponentContentValues singleComponentContentValues) {
        if (singleComponentContentValues == null) {
            return null;
        }
        LogUtil.i("DatabaseHelper", "insert()");
        ComponentInsertHelper componentInsertHelperBuildInsertHelper = ComponentInsertHelper.buildInsertHelper(this.mContext, singleComponentContentValues);
        if (componentInsertHelperBuildInsertHelper == null) {
            LogUtil.e("DatabaseHelper", "insert(): NOT supported component type");
            return null;
        }
        return componentInsertHelperBuildInsertHelper.insertContentValues(singleComponentContentValues);
    }

    public long getCalendarIdForAccount(String str) {
        long j;
        LogUtil.i("DatabaseHelper", "getCalendarIdForAccount()");
        if ("PC Sync".equals(str)) {
            return 1L;
        }
        Cursor cursorQuery = this.mContentResolver.query(CalendarContract.Calendars.CONTENT_URI, null, "account_name=\"" + str + "\"", null, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.getCount() > 0) {
                    cursorQuery.moveToFirst();
                    j = cursorQuery.getLong(cursorQuery.getColumnIndexOrThrow("_id"));
                } else {
                    j = -1;
                }
            } finally {
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
        }
    }
}
