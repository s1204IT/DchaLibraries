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
import com.android.calendar.R;
import com.android.calendar.Utils;

public class SelectSyncedCalendarsMultiAccountActivity extends ExpandableListActivity implements View.OnClickListener {
    private static final String[] PROJECTION = {"_id", "account_type", "account_name", "account_type || account_name AS ACCOUNT_KEY"};
    private MatrixCursor mAccountsCursor = null;
    private SelectSyncedCalendarsMultiAccountAdapter mAdapter;
    private ExpandableListView mList;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
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
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                SelectSyncedCalendarsMultiAccountActivity.this.mAccountsCursor = Utils.matrixCursorFromCursor(cursor);
                SelectSyncedCalendarsMultiAccountActivity.this.mAdapter = new SelectSyncedCalendarsMultiAccountAdapter(SelectSyncedCalendarsMultiAccountActivity.this.findViewById(R.id.calendars).getContext(), SelectSyncedCalendarsMultiAccountActivity.this.mAccountsCursor, SelectSyncedCalendarsMultiAccountActivity.this);
                SelectSyncedCalendarsMultiAccountActivity.this.mList.setAdapter(SelectSyncedCalendarsMultiAccountActivity.this.mAdapter);
                int count = SelectSyncedCalendarsMultiAccountActivity.this.mList.getCount();
                for (int i = 0; i < count; i++) {
                    SelectSyncedCalendarsMultiAccountActivity.this.mList.expandGroup(i);
                }
            }
        }.startQuery(0, null, CalendarContract.Calendars.CONTENT_URI, PROJECTION, "1) GROUP BY (ACCOUNT_KEY", null, "account_name");
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
    protected void onSaveInstanceState(Bundle outState) {
        boolean[] isExpanded;
        super.onSaveInstanceState(outState);
        this.mList = getExpandableListView();
        if (this.mList != null) {
            int count = this.mList.getCount();
            isExpanded = new boolean[count];
            for (int i = 0; i < count; i++) {
                isExpanded[i] = this.mList.isGroupExpanded(i);
            }
        } else {
            isExpanded = null;
        }
        outState.putBooleanArray("is_expanded", isExpanded);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        this.mList = getExpandableListView();
        boolean[] isExpanded = state.getBooleanArray("is_expanded");
        if (this.mList != null && isExpanded != null && this.mList.getCount() >= isExpanded.length) {
            for (int i = 0; i < isExpanded.length; i++) {
                if (isExpanded[i] && !this.mList.isGroupExpanded(i)) {
                    this.mList.expandGroup(i);
                } else if (!isExpanded[i] && this.mList.isGroupExpanded(i)) {
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Utils.returnToCalendarHome(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
