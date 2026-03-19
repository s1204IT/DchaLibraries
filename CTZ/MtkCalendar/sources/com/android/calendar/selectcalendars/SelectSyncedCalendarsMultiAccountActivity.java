package com.android.calendar.selectcalendars;

import android.app.ExpandableListActivity;
import android.content.AsyncQueryHandler;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import com.android.calendar.CalendarController;
import com.android.calendar.R;
import com.android.calendar.Utils;

public class SelectSyncedCalendarsMultiAccountActivity extends ExpandableListActivity implements View.OnClickListener {
    private static final String[] PROJECTION = {"_id", "account_type", "account_name", "account_type || account_name AS ACCOUNT_KEY"};
    private SelectSyncedCalendarsMultiAccountAdapter mAdapter;
    private ExpandableListView mList;
    private MatrixCursor mAccountsCursor = null;
    private String[] mSelectArgs = {"LOCAL"};

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.select_calendars_multi_accounts_fragment);
        this.mList = getExpandableListView();
        this.mList.setEmptyView(findViewById(R.id.loading));
        Utils.startCalendarMetafeedSync(null);
        findViewById(R.id.btn_done).setOnClickListener(this);
        findViewById(R.id.btn_discard).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_done) {
            if (this.mAdapter != null) {
                this.mAdapter.doSaveAction();
            }
            finish();
        } else if (view.getId() == R.id.btn_discard) {
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.mAdapter != null) {
            this.mAdapter.startRefreshStopDelay();
        }
        new AsyncQueryHandler(getContentResolver()) {
            @Override
            protected void onQueryComplete(int i, Object obj, Cursor cursor) {
                SelectSyncedCalendarsMultiAccountActivity.this.mAccountsCursor = Utils.matrixCursorFromCursor(cursor);
                if (cursor != null) {
                    cursor.close();
                }
                SelectSyncedCalendarsMultiAccountActivity.this.mAdapter = new SelectSyncedCalendarsMultiAccountAdapter(SelectSyncedCalendarsMultiAccountActivity.this.findViewById(R.id.calendars).getContext(), SelectSyncedCalendarsMultiAccountActivity.this.mAccountsCursor, SelectSyncedCalendarsMultiAccountActivity.this);
                SelectSyncedCalendarsMultiAccountActivity.this.mList.setAdapter(SelectSyncedCalendarsMultiAccountActivity.this.mAdapter);
                int count = SelectSyncedCalendarsMultiAccountActivity.this.mList.getCount();
                for (int i2 = 0; i2 < count; i2++) {
                    SelectSyncedCalendarsMultiAccountActivity.this.mList.expandGroup(i2);
                }
            }
        }.startQuery(0, null, CalendarContract.Calendars.CONTENT_URI, PROJECTION, "account_type!=?1) GROUP BY (ACCOUNT_KEY", this.mSelectArgs, "account_name");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.mAdapter != null) {
            this.mAdapter.cancelRefreshStopDelay();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (this.mAdapter != null) {
            this.mAdapter.closeChildrenCursors();
        }
        if (this.mAccountsCursor != null && !this.mAccountsCursor.isClosed()) {
            this.mAccountsCursor.close();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        boolean[] zArr;
        super.onSaveInstanceState(bundle);
        this.mList = getExpandableListView();
        if (this.mList != null) {
            int count = this.mList.getCount();
            zArr = new boolean[count];
            for (int i = 0; i < count; i++) {
                zArr[i] = this.mList.isGroupExpanded(i);
            }
        } else {
            zArr = null;
        }
        bundle.putBooleanArray("is_expanded", zArr);
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        this.mList = getExpandableListView();
        boolean[] booleanArray = bundle.getBooleanArray("is_expanded");
        if (this.mList != null && booleanArray != null && this.mList.getCount() >= booleanArray.length) {
            for (int i = 0; i < booleanArray.length; i++) {
                if (booleanArray[i] && !this.mList.isGroupExpanded(i)) {
                    this.mList.expandGroup(i);
                } else if (!booleanArray[i] && this.mList.isGroupExpanded(i)) {
                    this.mList.collapseGroup(i);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getActionBar().setDisplayOptions(4, 4);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            Utils.returnToCalendarHome(this);
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CalendarController.removeInstance(this);
    }
}
