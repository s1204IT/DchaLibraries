package com.android.providers.calendar;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.widget.SimpleAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarDebug extends ListActivity {
    private static final String[] CALENDARS_PROJECTION = {"_id", "calendar_displayName"};
    private static final String[] EVENTS_PROJECTION = {"_id"};
    private ListActivity mActivity;
    private ContentResolver mContentResolver;

    private class FetchInfoTask extends AsyncTask<Void, Void, List<Map<String, String>>> {
        private FetchInfoTask() {
        }

        @Override
        protected void onPreExecute() {
            CalendarDebug.this.setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected List<Map<String, String>> doInBackground(Void... voidArr) throws Throwable {
            ?? r5;
            Cursor cursorQuery;
            ArrayList arrayList = new ArrayList();
            ?? string = 0;
            string = 0;
            string = 0;
            try {
                try {
                    cursorQuery = CalendarDebug.this.mContentResolver.query(CalendarContract.Calendars.CONTENT_URI, CalendarDebug.CALENDARS_PROJECTION, null, null, "calendar_displayName");
                } catch (Throwable th) {
                    th = th;
                    r5 = string;
                }
            } catch (Exception e) {
                e = e;
            }
            try {
                if (cursorQuery == null) {
                    ?? r0 = CalendarDebug.this;
                    string = CalendarDebug.this.mActivity.getString(R.string.calendar_info_error);
                    r0.addItem(arrayList, string, "");
                } else {
                    while (cursorQuery.moveToNext()) {
                        int i = cursorQuery.getInt(0);
                        String string2 = cursorQuery.getString(1);
                        Cursor cursorQuery2 = CalendarDebug.this.mContentResolver.query(CalendarContract.Events.CONTENT_URI, CalendarDebug.EVENTS_PROJECTION, "calendar_id=" + i, null, null);
                        try {
                            int count = cursorQuery2.getCount();
                            cursorQuery2.close();
                            try {
                                int count2 = CalendarDebug.this.mContentResolver.query(CalendarContract.Events.CONTENT_URI, CalendarDebug.EVENTS_PROJECTION, "calendar_id=" + i + " AND dirty=1", null, null).getCount();
                                String string3 = count2 == 0 ? CalendarDebug.this.mActivity.getString(R.string.calendar_info_events, new Object[]{Integer.valueOf(count)}) : CalendarDebug.this.mActivity.getString(R.string.calendar_info_events_dirty, new Object[]{Integer.valueOf(count), Integer.valueOf(count2)});
                                string = CalendarDebug.this;
                                string.addItem(arrayList, string2, string3);
                            } finally {
                            }
                        } finally {
                        }
                    }
                }
                if (cursorQuery != null) {
                }
            } catch (Exception e2) {
                e = e2;
                string = cursorQuery;
                CalendarDebug.this.addItem(arrayList, CalendarDebug.this.mActivity.getString(R.string.calendar_info_error), e.toString());
                if (string != 0) {
                    string.close();
                }
                if (arrayList.size() == 0) {
                }
                return arrayList;
            } catch (Throwable th2) {
                th = th2;
                r5 = cursorQuery;
                if (r5 != 0) {
                    r5.close();
                }
                throw th;
            }
            if (arrayList.size() == 0) {
                CalendarDebug.this.addItem(arrayList, CalendarDebug.this.mActivity.getString(R.string.calendar_info_no_calendars), "");
            }
            return arrayList;
        }

        @Override
        protected void onPostExecute(List<Map<String, String>> list) {
            CalendarDebug.this.setProgressBarIndeterminateVisibility(false);
            CalendarDebug.this.setListAdapter(new SimpleAdapter(CalendarDebug.this.mActivity, list, android.R.layout.simple_list_item_2, new String[]{"title", "text"}, new int[]{android.R.id.text1, android.R.id.text2}));
        }
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        requestWindowFeature(5);
        this.mActivity = this;
        this.mContentResolver = getContentResolver();
        getListView();
        new FetchInfoTask().execute(new Void[0]);
    }

    protected void addItem(List<Map<String, String>> list, String str, String str2) {
        HashMap map = new HashMap();
        map.put("title", str);
        map.put("text", str2);
        list.add(map);
    }
}
