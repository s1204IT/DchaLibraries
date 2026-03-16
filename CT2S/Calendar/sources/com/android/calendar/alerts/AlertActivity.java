package com.android.calendar.alerts;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.TaskStackBuilder;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.android.calendar.AsyncQueryService;
import com.android.calendar.EventInfoActivity;
import com.android.calendar.R;
import com.android.calendar.Utils;
import com.android.calendar.alerts.GlobalDismissManager;
import java.util.LinkedList;
import java.util.List;

public class AlertActivity extends Activity implements View.OnClickListener {
    private static final String[] PROJECTION = {"_id", "title", "eventLocation", "allDay", "begin", "end", "event_id", "calendar_color", "rrule", "hasAlarm", "state", "alarmTime"};
    private static final String[] SELECTIONARG = {Integer.toString(1)};
    private AlertAdapter mAdapter;
    private Cursor mCursor;
    private Button mDismissAllButton;
    private ListView mListView;
    private QueryHandler mQueryHandler;
    private final AdapterView.OnItemClickListener mViewListener = new AdapterView.OnItemClickListener() {
        @Override
        @SuppressLint({"NewApi"})
        public void onItemClick(AdapterView<?> parent, View view, int position, long i) {
            AlertActivity alertActivity = AlertActivity.this;
            Cursor cursor = alertActivity.getItemForView(view);
            long alarmId = cursor.getLong(0);
            long eventId = cursor.getLong(6);
            long startMillis = cursor.getLong(4);
            AlertActivity.this.dismissAlarm(alarmId, eventId, startMillis);
            long endMillis = cursor.getLong(5);
            Intent eventIntent = AlertUtils.buildEventViewIntent(AlertActivity.this, eventId, startMillis, endMillis);
            if (Utils.isJellybeanOrLater()) {
                TaskStackBuilder.create(AlertActivity.this).addParentStack(EventInfoActivity.class).addNextIntent(eventIntent).startActivities();
            } else {
                alertActivity.startActivity(eventIntent);
            }
            alertActivity.finish();
        }
    };

    private void dismissFiredAlarms() {
        ContentValues values = new ContentValues(1);
        values.put(PROJECTION[10], (Integer) 2);
        this.mQueryHandler.startUpdate(0, null, CalendarContract.CalendarAlerts.CONTENT_URI, values, "state=1", null, 0L);
        if (this.mCursor == null) {
            Log.e("AlertActivity", "Unable to globally dismiss all notifications because cursor was null.");
            return;
        }
        if (this.mCursor.isClosed()) {
            Log.e("AlertActivity", "Unable to globally dismiss all notifications because cursor was closed.");
            return;
        }
        if (!this.mCursor.moveToFirst()) {
            Log.e("AlertActivity", "Unable to globally dismiss all notifications because cursor was empty.");
            return;
        }
        List<GlobalDismissManager.AlarmId> alarmIds = new LinkedList<>();
        do {
            long eventId = this.mCursor.getLong(6);
            long eventStart = this.mCursor.getLong(4);
            alarmIds.add(new GlobalDismissManager.AlarmId(eventId, eventStart));
        } while (this.mCursor.moveToNext());
        initiateGlobalDismiss(alarmIds);
    }

    private void dismissAlarm(long id, long eventId, long startTime) {
        ContentValues values = new ContentValues(1);
        values.put(PROJECTION[10], (Integer) 2);
        String selection = "_id=" + id;
        this.mQueryHandler.startUpdate(0, null, CalendarContract.CalendarAlerts.CONTENT_URI, values, selection, null, 0L);
        List<GlobalDismissManager.AlarmId> alarmIds = new LinkedList<>();
        alarmIds.add(new GlobalDismissManager.AlarmId(eventId, startTime));
        initiateGlobalDismiss(alarmIds);
    }

    private void initiateGlobalDismiss(List<GlobalDismissManager.AlarmId> alarmIds) {
        new AsyncTask<List<GlobalDismissManager.AlarmId>, Void, Void>() {
            @Override
            protected Void doInBackground(List<GlobalDismissManager.AlarmId>... params) {
                GlobalDismissManager.dismissGlobally(AlertActivity.this.getApplicationContext(), params[0]);
                return null;
            }
        }.execute(alarmIds);
    }

    private class QueryHandler extends AsyncQueryService {
        public QueryHandler(Context context) {
            super(context);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            if (!AlertActivity.this.isFinishing()) {
                AlertActivity.this.mCursor = cursor;
                AlertActivity.this.mAdapter.changeCursor(cursor);
                AlertActivity.this.mListView.setSelection(cursor.getCount() - 1);
                AlertActivity.this.mDismissAllButton.setEnabled(true);
                return;
            }
            cursor.close();
        }

        @Override
        protected void onUpdateComplete(int token, Object cookie, int result) {
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.alert_activity);
        setTitle(R.string.alert_title);
        this.mQueryHandler = new QueryHandler(this);
        this.mAdapter = new AlertAdapter(this, R.layout.alert_item);
        this.mListView = (ListView) findViewById(R.id.alert_container);
        this.mListView.setItemsCanFocus(true);
        this.mListView.setAdapter((ListAdapter) this.mAdapter);
        this.mListView.setOnItemClickListener(this.mViewListener);
        this.mDismissAllButton = (Button) findViewById(R.id.dismiss_all);
        this.mDismissAllButton.setOnClickListener(this);
        this.mDismissAllButton.setEnabled(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.mCursor == null) {
            Uri uri = CalendarContract.CalendarAlerts.CONTENT_URI_BY_INSTANCE;
            this.mQueryHandler.startQuery(0, null, uri, PROJECTION, "state=?", SELECTIONARG, "begin ASC,title ASC");
        } else if (!this.mCursor.requery()) {
            Log.w("AlertActivity", "Cursor#requery() failed.");
            this.mCursor.close();
            this.mCursor = null;
        }
    }

    void closeActivityIfEmpty() {
        if (this.mCursor != null && !this.mCursor.isClosed() && this.mCursor.getCount() == 0) {
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        new AsyncTask<Context, Void, Void>() {
            @Override
            protected Void doInBackground(Context... params) {
                AlertService.updateAlertNotification(params[0]);
                return null;
            }
        }.execute(this);
        if (this.mCursor != null) {
            this.mCursor.deactivate();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.mCursor != null) {
            this.mCursor.close();
        }
    }

    @Override
    public void onClick(View v) {
        if (v == this.mDismissAllButton) {
            NotificationManager nm = (NotificationManager) getSystemService("notification");
            nm.cancelAll();
            dismissFiredAlarms();
            finish();
        }
    }

    public Cursor getItemForView(View view) {
        int index = this.mListView.getPositionForView(view);
        if (index < 0) {
            return null;
        }
        return (Cursor) this.mListView.getAdapter().getItem(index);
    }
}
