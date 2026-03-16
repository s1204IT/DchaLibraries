package com.android.calendar.selectcalendars;

import android.accounts.Account;
import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.TextView;
import com.android.calendar.AsyncQueryService;
import com.android.calendar.R;
import com.android.calendar.Utils;
import com.android.calendar.selectcalendars.SelectCalendarsSyncAdapter;
import java.util.HashMap;

public class SelectCalendarsSyncFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener {
    private static final String[] PROJECTION = {"_id", "calendar_displayName", "calendar_color", "sync_events", "account_name", "account_type", "(account_name=ownerAccount) AS \"primary\""};
    private Account mAccount;
    private Button mAccountsButton;
    private AsyncQueryService mService;
    private TextView mSyncStatus;
    private final String[] mArgs = new String[2];
    private Handler mHandler = new Handler();
    private ContentObserver mCalendarsObserver = new ContentObserver(this.mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            if (!selfChange) {
                SelectCalendarsSyncFragment.this.getLoaderManager().initLoader(0, null, SelectCalendarsSyncFragment.this);
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.account_calendars, (ViewGroup) null);
        this.mSyncStatus = (TextView) v.findViewById(R.id.account_status);
        this.mSyncStatus.setVisibility(8);
        this.mAccountsButton = (Button) v.findViewById(R.id.sync_settings);
        this.mAccountsButton.setVisibility(8);
        this.mAccountsButton.setOnClickListener(this);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setEmptyText(getActivity().getText(R.string.no_syncable_calendars));
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!ContentResolver.getMasterSyncAutomatically() || !ContentResolver.getSyncAutomatically(this.mAccount, "com.android.calendar")) {
            Resources res = getActivity().getResources();
            this.mSyncStatus.setText(res.getString(R.string.acct_not_synced));
            this.mSyncStatus.setVisibility(0);
            this.mAccountsButton.setText(res.getString(R.string.accounts));
            this.mAccountsButton.setVisibility(0);
            return;
        }
        this.mSyncStatus.setVisibility(8);
        this.mAccountsButton.setVisibility(8);
        Utils.startCalendarMetafeedSync(this.mAccount);
        getActivity().getContentResolver().registerContentObserver(CalendarContract.Calendars.CONTENT_URI, true, this.mCalendarsObserver);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mService = new AsyncQueryService(activity);
        Bundle bundle = getArguments();
        if (bundle != null && bundle.containsKey("account_name") && bundle.containsKey("account_type")) {
            this.mAccount = new Account(bundle.getString("account_name"), bundle.getString("account_type"));
        }
    }

    @Override
    public void onPause() {
        HashMap<Long, SelectCalendarsSyncAdapter.CalendarRow> changes;
        ListAdapter listAdapter = getListAdapter();
        if (listAdapter != null && (changes = ((SelectCalendarsSyncAdapter) listAdapter).getChanges()) != null && changes.size() > 0) {
            for (SelectCalendarsSyncAdapter.CalendarRow row : changes.values()) {
                if (row.synced != row.originalSynced) {
                    long id = row.id;
                    this.mService.cancelOperation((int) id);
                    Uri uri = ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, row.id);
                    ContentValues values = new ContentValues();
                    int synced = row.synced ? 1 : 0;
                    values.put("sync_events", Integer.valueOf(synced));
                    values.put("visible", Integer.valueOf(synced));
                    this.mService.startUpdate((int) id, null, uri, values, null, null, 0L);
                }
            }
            changes.clear();
        }
        getActivity().getContentResolver().unregisterContentObserver(this.mCalendarsObserver);
        super.onPause();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        this.mArgs[0] = this.mAccount.name;
        this.mArgs[1] = this.mAccount.type;
        return new CursorLoader(getActivity(), CalendarContract.Calendars.CONTENT_URI, PROJECTION, "account_name=? AND account_type=?", this.mArgs, "\"primary\" DESC,calendar_displayName COLLATE NOCASE");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        SelectCalendarsSyncAdapter adapter = (SelectCalendarsSyncAdapter) getListAdapter();
        if (adapter == null) {
            adapter = new SelectCalendarsSyncAdapter(getActivity(), data, getFragmentManager());
            setListAdapter(adapter);
        } else {
            adapter.changeCursor(data);
        }
        getListView().setOnItemClickListener(adapter);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        setListAdapter(null);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent();
        intent.setAction("android.settings.SYNC_SETTINGS");
        getActivity().startActivity(intent);
    }
}
