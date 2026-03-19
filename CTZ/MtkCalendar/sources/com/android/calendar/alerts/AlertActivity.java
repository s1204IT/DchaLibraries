package com.android.calendar.alerts;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.TaskStackBuilder;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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
import com.android.calendar.CalendarController;
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
    private boolean mDoUpdate = false;
    private final AdapterView.OnItemClickListener mViewListener = new AdapterView.OnItemClickListener() {
        @Override
        @SuppressLint({"NewApi"})
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
            AlertActivity alertActivity = AlertActivity.this;
            Cursor itemForView = alertActivity.getItemForView(view);
            long j2 = itemForView.getLong(0);
            long j3 = itemForView.getLong(6);
            long j4 = itemForView.getLong(4);
            AlertActivity.this.dismissAlarm(j2, j3, j4);
            Intent intentBuildEventViewIntent = AlertUtils.buildEventViewIntent(AlertActivity.this, j3, j4, itemForView.getLong(5));
            if (Utils.isJellybeanOrLater()) {
                TaskStackBuilder.create(AlertActivity.this).addParentStack(EventInfoActivity.class).addNextIntent(intentBuildEventViewIntent).startActivities();
            } else {
                alertActivity.startActivity(intentBuildEventViewIntent);
            }
            AlertActivity.this.mDoUpdate = true;
            alertActivity.finish();
        }
    };

    private void dismissFiredAlarms() {
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(PROJECTION[10], (Integer) 2);
        this.mQueryHandler.startUpdate(0, null, CalendarContract.CalendarAlerts.CONTENT_URI, contentValues, "state=1", null, 0L);
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
        LinkedList linkedList = new LinkedList();
        do {
            linkedList.add(new GlobalDismissManager.AlarmId(this.mCursor.getLong(6), this.mCursor.getLong(4)));
        } while (this.mCursor.moveToNext());
        initiateGlobalDismiss(linkedList);
    }

    private void dismissAlarm(long j, long j2, long j3) {
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(PROJECTION[10], (Integer) 2);
        this.mQueryHandler.startUpdate(0, null, CalendarContract.CalendarAlerts.CONTENT_URI, contentValues, "_id=" + j, null, 0L);
        LinkedList linkedList = new LinkedList();
        linkedList.add(new GlobalDismissManager.AlarmId(j2, j3));
        initiateGlobalDismiss(linkedList);
    }

    private void initiateGlobalDismiss(List<GlobalDismissManager.AlarmId> list) {
        new AsyncTask<List<GlobalDismissManager.AlarmId>, Void, Void>() {
            @Override
            protected Void doInBackground(List<GlobalDismissManager.AlarmId>... listArr) {
                GlobalDismissManager.dismissGlobally(AlertActivity.this.getApplicationContext(), listArr[0]);
                return null;
            }
        }.execute(list);
    }

    private class QueryHandler extends AsyncQueryService {
        public QueryHandler(Context context) {
            super(context);
        }

        @Override
        protected void onQueryComplete(int i, Object obj, Cursor cursor) {
            if (cursor == null) {
                return;
            }
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
        protected void onUpdateComplete(int i, Object obj, int i2) {
        }
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
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
            this.mQueryHandler.startQuery(0, null, CalendarContract.CalendarAlerts.CONTENT_URI_BY_INSTANCE, PROJECTION, "state=?", SELECTIONARG, "begin ASC,title ASC");
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
        if (this.mDoUpdate) {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), AlertReceiver.class);
            intent.putExtra("action", "android.intent.action.EVENT_REMINDER");
            intent.setAction("android.intent.action.EVENT_REMINDER");
            sendBroadcast(intent);
        }
        if (this.mCursor != null) {
            this.mCursor.deactivate();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.mCursor != null) {
            this.mCursor.close();
        }
        CalendarController.removeInstance(this);
    }

    @Override
    public void onClick(View view) {
        if (view == this.mDismissAllButton) {
            ((NotificationManager) getSystemService("notification")).cancelAll();
            dismissFiredAlarms();
            this.mDoUpdate = true;
            finish();
        }
    }

    public Cursor getItemForView(View view) {
        int positionForView = this.mListView.getPositionForView(view);
        if (positionForView < 0) {
            return null;
        }
        return (Cursor) this.mListView.getAdapter().getItem(positionForView);
    }
}
