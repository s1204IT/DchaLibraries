package com.android.calendar.selectcalendars;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
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

    public SelectVisibleCalendarsFragment(int itemLayout) {
        mCalendarItemLayout = itemLayout;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity;
        this.mController = CalendarController.getInstance(activity);
        this.mController.registerEventHandler(R.layout.select_calendars_fragment, this);
        this.mService = new AsyncQueryService(activity) {
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                SelectVisibleCalendarsFragment.this.mAdapter.changeCursor(cursor);
                SelectVisibleCalendarsFragment.this.mCursor = cursor;
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
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        this.mView = inflater.inflate(R.layout.select_calendars_fragment, (ViewGroup) null);
        this.mList = (ListView) this.mView.findViewById(R.id.list);
        if (Utils.getConfigBool(getActivity(), R.bool.multiple_pane_config)) {
            this.mList.setDivider(null);
            View v = this.mView.findViewById(R.id.manage_sync_set);
            if (v != null) {
                v.setVisibility(8);
            }
        }
        return this.mView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.mAdapter = new SelectCalendarsSimpleAdapter(this.mContext, mCalendarItemLayout, null, getFragmentManager());
        this.mList.setAdapter((ListAdapter) this.mAdapter);
        this.mList.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (this.mAdapter != null && this.mAdapter.getCount() > position) {
            toggleVisibility(position);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mQueryToken = this.mService.getNextToken();
        this.mService.startQuery(mQueryToken, null, CalendarContract.Calendars.CONTENT_URI, PROJECTION, "sync_events=?", SELECTION_ARGS, "account_name");
    }

    public void toggleVisibility(int position) {
        mUpdateToken = this.mService.getNextToken();
        Uri uri = ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, this.mAdapter.getItemId(position));
        ContentValues values = new ContentValues();
        int visibility = this.mAdapter.getVisible(position) ^ 1;
        values.put("visible", Integer.valueOf(visibility));
        this.mService.startUpdate(mUpdateToken, null, uri, values, null, null, 0L);
        this.mAdapter.setVisible(position, visibility);
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
    public void handleEvent(CalendarController.EventInfo event) {
        eventsChanged();
    }

    @Override
    public void onCalendarColorsLoaded() {
        if (this.mAdapter != null) {
            this.mAdapter.notifyDataSetChanged();
        }
    }
}
