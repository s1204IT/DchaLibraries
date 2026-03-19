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
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.android.calendar.AsyncQueryService;
import com.android.calendar.R;
import com.android.calendar.Utils;
import com.android.calendar.selectcalendars.SelectCalendarsSyncAdapter;
import java.util.HashMap;

public class SelectCalendarsSyncFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener {
    private Account mAccount;
    private Button mAccountsButton;
    private AsyncQueryService mService;
    private TextView mSyncStatus;
    private static final String[] PROJECTION = {"_id", "calendar_displayName", "calendar_color", "sync_events", "account_name", "account_type", "(account_name=ownerAccount) AS \"primary\""};
    private static final String[] CALENDAR_PERMISSION = {"android.permission.READ_CALENDAR", "android.permission.WRITE_CALENDAR"};
    private final String[] mArgs = new String[2];
    private Handler mHandler = new Handler();
    private ContentObserver mCalendarsObserver = new ContentObserver(this.mHandler) {
        @Override
        public void onChange(boolean z) {
            if (!z) {
                SelectCalendarsSyncFragment.this.getLoaderManager().initLoader(0, null, SelectCalendarsSyncFragment.this);
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        if (!checkPermissions()) {
            return null;
        }
        View viewInflate = layoutInflater.inflate(R.layout.account_calendars, (ViewGroup) null);
        this.mSyncStatus = (TextView) viewInflate.findViewById(R.id.account_status);
        this.mSyncStatus.setVisibility(8);
        this.mAccountsButton = (Button) viewInflate.findViewById(R.id.sync_settings);
        this.mAccountsButton.setVisibility(8);
        this.mAccountsButton.setOnClickListener(this);
        return viewInflate;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        if (!checkPermissions()) {
            return;
        }
        setEmptyText(getActivity().getText(R.string.no_syncable_calendars));
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!checkPermissions()) {
            Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.denied_required_permission), 1).show();
            getActivity().finish();
            return;
        }
        if (!ContentResolver.getMasterSyncAutomatically() || !ContentResolver.getSyncAutomatically(this.mAccount, "com.android.calendar")) {
            Resources resources = getActivity().getResources();
            this.mSyncStatus.setText(resources.getString(R.string.acct_not_synced));
            this.mSyncStatus.setVisibility(0);
            this.mAccountsButton.setText(resources.getString(R.string.accounts));
            this.mAccountsButton.setVisibility(0);
            return;
        }
        this.mSyncStatus.setVisibility(8);
        this.mAccountsButton.setVisibility(8);
        Utils.startCalendarMetafeedSync(this.mAccount);
        getActivity().getContentResolver().registerContentObserver(CalendarContract.Calendars.CONTENT_URI, true, this.mCalendarsObserver);
    }

    protected boolean hasRequiredPermission(String[] strArr) {
        for (String str : strArr) {
            if (getActivity().checkSelfPermission(str) != 0) {
                return false;
            }
        }
        return true;
    }

    private boolean checkPermissions() {
        if (!hasRequiredPermission(CALENDAR_PERMISSION)) {
            return false;
        }
        return true;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!checkPermissions()) {
            Toast.makeText(activity.getApplicationContext(), getResources().getString(R.string.denied_required_permission), 1).show();
            activity.finish();
            return;
        }
        this.mService = new AsyncQueryService(activity);
        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey("account_name") && arguments.containsKey("account_type")) {
            this.mAccount = new Account(arguments.getString("account_name"), arguments.getString("account_type"));
        }
    }

    @Override
    public void onPause() {
        HashMap<Long, SelectCalendarsSyncAdapter.CalendarRow> changes;
        ListAdapter listAdapter = getListAdapter();
        if (listAdapter != null && (changes = ((SelectCalendarsSyncAdapter) listAdapter).getChanges()) != null && changes.size() > 0) {
            for (SelectCalendarsSyncAdapter.CalendarRow calendarRow : changes.values()) {
                if (calendarRow.synced != calendarRow.originalSynced) {
                    int i = (int) calendarRow.id;
                    this.mService.cancelOperation(i);
                    Uri uriWithAppendedId = ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calendarRow.id);
                    ContentValues contentValues = new ContentValues();
                    boolean z = calendarRow.synced;
                    contentValues.put("sync_events", Integer.valueOf(z ? 1 : 0));
                    contentValues.put("visible", Integer.valueOf(z ? 1 : 0));
                    this.mService.startUpdate(i, null, uriWithAppendedId, contentValues, null, null, 0L);
                }
            }
            changes.clear();
            SelectCalendarsSyncAdapter.clearCheckBoxStatus();
        }
        getActivity().getContentResolver().unregisterContentObserver(this.mCalendarsObserver);
        super.onPause();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        this.mArgs[0] = this.mAccount.name;
        this.mArgs[1] = this.mAccount.type;
        return new CursorLoader(getActivity(), CalendarContract.Calendars.CONTENT_URI, PROJECTION, "account_name=? AND account_type=?", this.mArgs, "\"primary\" DESC,calendar_displayName COLLATE NOCASE");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        SelectCalendarsSyncAdapter selectCalendarsSyncAdapter = (SelectCalendarsSyncAdapter) getListAdapter();
        if (selectCalendarsSyncAdapter == null) {
            selectCalendarsSyncAdapter = new SelectCalendarsSyncAdapter(getActivity(), cursor, getFragmentManager());
            setListAdapter(selectCalendarsSyncAdapter);
        } else {
            selectCalendarsSyncAdapter.changeCursor(cursor);
        }
        getListView().setOnItemClickListener(selectCalendarsSyncAdapter);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        setListAdapter(null);
    }

    @Override
    public void onClick(View view) {
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        Intent intent = new Intent();
        intent.setAction("android.settings.SYNC_SETTINGS");
        intent.putExtra("authorities", new String[]{"com.android.calendar"});
        getActivity().startActivity(intent);
    }
}
