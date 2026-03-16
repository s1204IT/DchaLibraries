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

    public CalendarColorCache(Context context, OnCalendarColorsLoadedListener listener) {
        this.mListener = listener;
        this.mService = new AsyncQueryService(context) {
            @Override
            public void onQueryComplete(int token, Object cookie, Cursor c) {
                if (c != null) {
                    if (c.moveToFirst()) {
                        CalendarColorCache.this.clear();
                        do {
                            CalendarColorCache.this.insert(c.getString(0), c.getString(1));
                        } while (c.moveToNext());
                        CalendarColorCache.this.mListener.onCalendarColorsLoaded();
                    }
                    if (c != null) {
                        c.close();
                    }
                }
            }
        };
        this.mService.startQuery(0, null, CalendarContract.Colors.CONTENT_URI, PROJECTION, "color_type=0", null, null);
    }

    private void insert(String accountName, String accountType) {
        this.mCache.add(generateKey(accountName, accountType));
    }

    public boolean hasColors(String accountName, String accountType) {
        return this.mCache.contains(generateKey(accountName, accountType));
    }

    private void clear() {
        this.mCache.clear();
    }

    private String generateKey(String accountName, String accountType) {
        this.mStringBuffer.setLength(0);
        return this.mStringBuffer.append(accountName).append("::").append(accountType).toString();
    }
}
