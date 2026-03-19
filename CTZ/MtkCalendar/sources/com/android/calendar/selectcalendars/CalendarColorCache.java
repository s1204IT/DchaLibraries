package com.android.calendar.selectcalendars;

import android.content.Context;
import android.database.Cursor;
import android.provider.CalendarContract;
import com.android.calendar.AsyncQueryService;
import java.util.HashSet;

public class CalendarColorCache {
    private static String[] PROJECTION = {"account_name", "account_type"};
    private OnCalendarColorsLoadedListener mListener;
    private AsyncQueryService mService;
    private HashSet<String> mCache = new HashSet<>();
    private StringBuffer mStringBuffer = new StringBuffer();

    public interface OnCalendarColorsLoadedListener {
        void onCalendarColorsLoaded();
    }

    public CalendarColorCache(Context context, OnCalendarColorsLoadedListener onCalendarColorsLoadedListener) {
        this.mListener = onCalendarColorsLoadedListener;
        this.mService = new AsyncQueryService(context) {
            @Override
            public void onQueryComplete(int i, Object obj, Cursor cursor) {
                if (cursor == null) {
                    return;
                }
                if (cursor.moveToFirst()) {
                    CalendarColorCache.this.clear();
                    do {
                        CalendarColorCache.this.insert(cursor.getString(0), cursor.getString(1));
                    } while (cursor.moveToNext());
                    CalendarColorCache.this.mListener.onCalendarColorsLoaded();
                }
                if (cursor != null) {
                    cursor.close();
                }
            }
        };
        this.mService.startQuery(0, null, CalendarContract.Colors.CONTENT_URI, PROJECTION, "color_type=0", null, null);
    }

    private void insert(String str, String str2) {
        this.mCache.add(generateKey(str, str2));
    }

    public boolean hasColors(String str, String str2) {
        return this.mCache.contains(generateKey(str, str2));
    }

    private void clear() {
        this.mCache.clear();
    }

    private String generateKey(String str, String str2) {
        this.mStringBuffer.setLength(0);
        StringBuffer stringBuffer = this.mStringBuffer;
        stringBuffer.append(str);
        stringBuffer.append("::");
        stringBuffer.append(str2);
        return stringBuffer.toString();
    }
}
