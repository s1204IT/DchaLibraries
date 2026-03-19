package com.mediatek.vcalendar.database;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import com.mediatek.vcalendar.SingleComponentCursorInfo;
import com.mediatek.vcalendar.utils.CursorUtil;
import com.mediatek.vcalendar.utils.LogUtil;

public abstract class ComponentInfoHelper {
    private static final String TAG = "ComponentInfoHelper";
    private int mComponentCount = -1;
    private Cursor mComponentCursor;
    protected final ContentResolver mContentResolver;
    protected final Context mContext;
    protected String mType;

    protected abstract boolean buildComponentInfo(Cursor cursor, SingleComponentCursorInfo singleComponentCursorInfo);

    protected ComponentInfoHelper(Context context, String str) {
        this.mContext = context;
        this.mContentResolver = this.mContext.getContentResolver();
        this.mType = str;
    }

    static ComponentInfoHelper createComponentInfoHelper(Context context, String str) {
        LogUtil.d(TAG, "createComponentInfoHelper(): type URI: " + str);
        if (str.equals(CalendarContract.Events.CONTENT_URI.toString())) {
            return new VEventInfoHelper(context, "VEVENT");
        }
        return null;
    }

    public boolean query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        Cursor cursorQuery = this.mContentResolver.query(uri, strArr, str, strArr2, str2);
        if (cursorQuery == null) {
            return false;
        }
        this.mComponentCursor = CursorUtil.copyOneCursor(cursorQuery);
        this.mComponentCount = this.mComponentCursor.getCount();
        cursorQuery.close();
        if (!this.mComponentCursor.moveToFirst()) {
            return false;
        }
        return true;
    }

    public int getComponentCount() {
        return this.mComponentCount;
    }

    public boolean hasNextComponentInfo() {
        if (this.mComponentCursor == null || this.mComponentCursor.isAfterLast()) {
            return false;
        }
        return true;
    }

    public SingleComponentCursorInfo getNextComponentInfo() {
        SingleComponentCursorInfo singleComponentCursorInfo = new SingleComponentCursorInfo();
        if (!buildComponentInfo(this.mComponentCursor, singleComponentCursorInfo)) {
            return null;
        }
        this.mComponentCursor.moveToNext();
        return singleComponentCursorInfo;
    }
}
