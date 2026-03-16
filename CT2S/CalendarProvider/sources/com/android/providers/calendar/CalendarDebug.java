package com.android.providers.calendar;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.widget.ListAdapter;
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
        protected List<Map<String, String>> doInBackground(Void... params) {
            Cursor cursor = null;
            List<Map<String, String>> items = new ArrayList<>();
            try {
                try {
                    Cursor dirtyCursor = CalendarDebug.this.mContentResolver.query(CalendarContract.Calendars.CONTENT_URI, CalendarDebug.CALENDARS_PROJECTION, null, null, "calendar_displayName");
                    if (dirtyCursor == null) {
                        CalendarDebug.this.addItem(items, CalendarDebug.this.mActivity.getString(R.string.calendar_info_error), "");
                    } else {
                        while (dirtyCursor.moveToNext()) {
                            int id = dirtyCursor.getInt(0);
                            String displayName = dirtyCursor.getString(1);
                            String where = "calendar_id=" + id;
                            dirtyCursor = CalendarDebug.this.mContentResolver.query(CalendarContract.Events.CONTENT_URI, CalendarDebug.EVENTS_PROJECTION, where, null, null);
                            try {
                                int eventCount = dirtyCursor.getCount();
                                dirtyCursor.close();
                                String dirtyWhere = "calendar_id=" + id + " AND dirty=1";
                                dirtyCursor = CalendarDebug.this.mContentResolver.query(CalendarContract.Events.CONTENT_URI, CalendarDebug.EVENTS_PROJECTION, dirtyWhere, null, null);
                                try {
                                    int dirtyCount = dirtyCursor.getCount();
                                    String text = dirtyCount == 0 ? CalendarDebug.this.mActivity.getString(R.string.calendar_info_events, new Object[]{Integer.valueOf(eventCount)}) : CalendarDebug.this.mActivity.getString(R.string.calendar_info_events_dirty, new Object[]{Integer.valueOf(eventCount), Integer.valueOf(dirtyCount)});
                                    CalendarDebug.this.addItem(items, displayName, text);
                                } finally {
                                }
                            } finally {
                            }
                        }
                    }
                } catch (Exception e) {
                    CalendarDebug.this.addItem(items, CalendarDebug.this.mActivity.getString(R.string.calendar_info_error), e.toString());
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                if (items.size() == 0) {
                    CalendarDebug.this.addItem(items, CalendarDebug.this.mActivity.getString(R.string.calendar_info_no_calendars), "");
                }
                return items;
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        @Override
        protected void onPostExecute(List<Map<String, String>> items) {
            CalendarDebug.this.setProgressBarIndeterminateVisibility(false);
            ListAdapter adapter = new SimpleAdapter(CalendarDebug.this.mActivity, items, android.R.layout.simple_list_item_2, new String[]{"title", "text"}, new int[]{android.R.id.text1, android.R.id.text2});
            CalendarDebug.this.setListAdapter(adapter);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(5);
        this.mActivity = this;
        this.mContentResolver = getContentResolver();
        getListView();
        new FetchInfoTask().execute(new Void[0]);
    }

    protected void addItem(List<Map<String, String>> items, String title, String text) {
        Map<String, String> itemMap = new HashMap<>();
        itemMap.put("title", title);
        itemMap.put("text", text);
        items.add(itemMap);
    }
}
