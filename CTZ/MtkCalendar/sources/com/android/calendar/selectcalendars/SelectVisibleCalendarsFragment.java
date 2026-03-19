package com.android.calendar.selectcalendars;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.android.calendar.AsyncQueryService;
import com.android.calendar.CalendarController;
import com.android.calendar.R;
import com.android.calendar.Utils;
import com.android.calendar.selectcalendars.CalendarColorCache;

public class SelectVisibleCalendarsFragment extends Fragment implements AdapterView.OnItemClickListener, CalendarController.EventHandler, CalendarColorCache.OnCalendarColorsLoadedListener {
    private static int mQueryToken;
    private static int mUpdateToken;
    private SelectCalendarsSimpleAdapter mAdapter;
    private Activity mContext;
    private CalendarController mController;
    private Cursor mCursor;
    private ListView mList;
    private AsyncQueryService mService;
    private View mView = null;
    private static final String[] SELECTION_ARGS = {"1"};
    private static final String[] PROJECTION = {"_id", "account_name", "account_type", "ownerAccount", "calendar_displayName", "calendar_color", "visible", "sync_events", "(account_name=ownerAccount) AS \"primary\""};
    private static int mCalendarItemLayout = R.layout.mini_calendar_item;

    public SelectVisibleCalendarsFragment() {
    }

    public SelectVisibleCalendarsFragment(int i) {
        mCalendarItemLayout = i;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity;
        this.mController = CalendarController.getInstance(activity);
        this.mController.registerEventHandler(R.layout.select_calendars_fragment, this);
        this.mService = new AsyncQueryService(activity) {
            @Override
            protected void onQueryComplete(int i, Object obj, Cursor cursor) {
                SelectVisibleCalendarsFragment.this.mAdapter.changeCursor(cursor);
                SelectVisibleCalendarsFragment.this.mCursor = cursor;
                if (cursor == null) {
                    return;
                }
                int columnIndex = cursor.getColumnIndex("account_type");
                View viewFindViewById = SelectVisibleCalendarsFragment.this.mView.findViewById(R.id.manage_sync_set);
                if (cursor.moveToFirst()) {
                    while (columnIndex != -1 && "LOCAL".equals(cursor.getString(columnIndex))) {
                        viewFindViewById.setEnabled(false);
                        if (!cursor.moveToNext()) {
                            return;
                        }
                    }
                    Log.e("Calendar", "the colume do not exsit or it is not local account");
                    viewFindViewById.setEnabled(true);
                }
            }
        };
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.mController.deregisterEventHandler(Integer.valueOf(R.layout.select_calendars_fragment));
        if (this.mCursor != null) {
            this.mAdapter.changeCursor(null);
            this.mCursor.close();
            this.mCursor = null;
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        super.onCreateView(layoutInflater, viewGroup, bundle);
        this.mView = layoutInflater.inflate(R.layout.select_calendars_fragment, (ViewGroup) null);
        this.mList = (ListView) this.mView.findViewById(R.id.list);
        if (Utils.getConfigBool(getActivity(), R.bool.multiple_pane_config)) {
            this.mList.setDivider(null);
            View viewFindViewById = this.mView.findViewById(R.id.manage_sync_set);
            if (viewFindViewById != null) {
                viewFindViewById.setVisibility(8);
            }
        }
        return this.mView;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        this.mAdapter = new SelectCalendarsSimpleAdapter(this.mContext, mCalendarItemLayout, null, getFragmentManager());
        this.mList.setAdapter((ListAdapter) this.mAdapter);
        this.mList.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
        if (this.mAdapter == null || this.mAdapter.getCount() <= i) {
            return;
        }
        toggleVisibility(i);
    }

    @Override
    public void onResume() {
        super.onResume();
        mQueryToken = this.mService.getNextToken();
        this.mService.startQuery(mQueryToken, null, CalendarContract.Calendars.CONTENT_URI, PROJECTION, "sync_events=?", SELECTION_ARGS, "account_name");
    }

    public void toggleVisibility(int i) {
        mUpdateToken = this.mService.getNextToken();
        Uri uriWithAppendedId = ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, this.mAdapter.getItemId(i));
        ContentValues contentValues = new ContentValues();
        int visible = this.mAdapter.getVisible(i) ^ 1;
        contentValues.put("visible", Integer.valueOf(visible));
        this.mService.startUpdate(mUpdateToken, null, uriWithAppendedId, contentValues, null, null, 0L);
        this.mAdapter.setVisible(i, visible);
    }

    public void eventsChanged() {
        if (this.mService != null) {
            this.mService.cancelOperation(mQueryToken);
            mQueryToken = this.mService.getNextToken();
            this.mService.startQuery(mQueryToken, null, CalendarContract.Calendars.CONTENT_URI, PROJECTION, "sync_events=?", SELECTION_ARGS, "account_name");
        }
    }

    @Override
    public long getSupportedEventTypes() {
        return 128L;
    }

    @Override
    public void handleEvent(CalendarController.EventInfo eventInfo) {
        if (eventInfo.eventType == 128) {
            eventsChanged();
        }
    }

    @Override
    public void onCalendarColorsLoaded() {
        if (this.mAdapter != null) {
            this.mAdapter.notifyDataSetChanged();
        }
    }
}
