package com.mediatek.vcalendar.database;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import com.mediatek.vcalendar.SingleComponentContentValues;
import com.mediatek.vcalendar.SingleComponentCursorInfo;
import com.mediatek.vcalendar.utils.LogUtil;
import java.util.List;

public class DatabaseHelper {
    public static final String ACCOUNT_PC_SYNC = "PC Sync";
    private static final String COLUMN_ID = "_id";
    private static final boolean DEBUG = false;
    private static final String TAG = "DatabaseHelper";
    private ComponentInfoHelper mComponentInfoHelper;
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
        LogUtil.i(TAG, "insert()");
        ComponentInsertHelper componentInsertHelperBuildInsertHelper = ComponentInsertHelper.buildInsertHelper(this.mContext, singleComponentContentValues);
        if (componentInsertHelperBuildInsertHelper == null) {
            LogUtil.e(TAG, "insert(): NOT supported component type");
            return null;
        }
        return componentInsertHelperBuildInsertHelper.insertContentValues(singleComponentContentValues);
    }

    public Uri insert(List<SingleComponentContentValues> list) {
        LogUtil.i(TAG, "insert() multiComponentContentValues");
        if (list.size() <= 0) {
            return null;
        }
        ComponentInsertHelper componentInsertHelperBuildInsertHelper = ComponentInsertHelper.buildInsertHelper(this.mContext, list.get(0));
        if (componentInsertHelperBuildInsertHelper == null) {
            LogUtil.e(TAG, "insert(): NOT supported component type");
            return null;
        }
        return componentInsertHelperBuildInsertHelper.insertMultiComponentContentValues(list);
    }

    public boolean query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        LogUtil.i(TAG, "query(): selection = \"" + str + "\"");
        this.mComponentInfoHelper = ComponentInfoHelper.createComponentInfoHelper(this.mContext, uri.toString());
        if (this.mComponentInfoHelper == null) {
            LogUtil.e(TAG, "query(): NOT supported URI: " + uri.toString());
            return DEBUG;
        }
        return this.mComponentInfoHelper.query(uri, strArr, str, strArr2, str2);
    }

    public int getComponentCount() {
        if (this.mComponentInfoHelper == null) {
            LogUtil.e(TAG, "getComponentCount(): MUST query first");
            return -1;
        }
        return this.mComponentInfoHelper.getComponentCount();
    }

    public boolean hasNextComponentInfo() {
        if (this.mComponentInfoHelper == null) {
            LogUtil.e(TAG, "hasNextComponentInfo(): MUST query first");
            return DEBUG;
        }
        return this.mComponentInfoHelper.hasNextComponentInfo();
    }

    public SingleComponentCursorInfo getNextComponentInfo() {
        if (this.mComponentInfoHelper == null) {
            LogUtil.e(TAG, "getNextComponentInfo(): MUST query first");
            return null;
        }
        return this.mComponentInfoHelper.getNextComponentInfo();
    }

    public long getCalendarIdForAccount(String str) {
        long j;
        LogUtil.i(TAG, "getCalendarIdForAccount()");
        if (ACCOUNT_PC_SYNC.equals(str)) {
            return 1L;
        }
        Cursor cursorQuery = this.mContentResolver.query(CalendarContract.Calendars.CONTENT_URI, null, "account_name=\"" + str + "\"", null, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.getCount() > 0) {
                    cursorQuery.moveToFirst();
                    j = cursorQuery.getLong(cursorQuery.getColumnIndexOrThrow(COLUMN_ID));
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
