package com.mediatek.calendar.clearevents;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;
import com.android.calendar.AllInOneActivity;
import com.android.calendar.AsyncQueryService;
import com.android.calendar.R;
import com.android.calendar.Utils;
import com.android.calendar.selectcalendars.SelectCalendarsSimpleAdapter;
import com.mediatek.calendar.LogUtil;
import java.util.ArrayList;
import java.util.Iterator;

public class SelectClearableCalendarsFragment extends Fragment implements AdapterView.OnItemClickListener {
    private static int sDeleteToken;
    private SelectCalendarsSimpleAdapter mAdapter;
    private AlertDialog mAlertDialog;
    private Button mBtnDelete;
    private Activity mContext;
    private Cursor mCursor;
    private ListView mList;
    private AsyncQueryService mService;
    private Toast mToast;
    private View mView;
    private int sQueryToken;
    private static final String[] SELECTION_ARGS = {"1"};
    private static final String[] PROJECTION = {"_id", "account_name", "account_type", "ownerAccount", "calendar_displayName", "calendar_color", "visible", "sync_events", "(account_name=ownerAccount) AS \"primary\""};
    private static int mCalendarItemLayout = R.layout.mini_calendar_item;
    private ArrayList<Long> mCalendarIds = new ArrayList<>();
    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btn_cancel:
                    LogUtil.d("Calendar", "Clear all events, cancel");
                    SelectClearableCalendarsFragment.this.mContext.finish();
                    break;
                case R.id.btn_ok:
                    LogUtil.d("Calendar", "Clear all events, ok");
                    AlertDialog alertDialogCreate = new AlertDialog.Builder(SelectClearableCalendarsFragment.this.mContext).setTitle(R.string.delete_label).setMessage(R.string.clear_all_selected_events_title).setIconAttribute(android.R.attr.alertDialogIcon).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).create();
                    alertDialogCreate.setButton(-1, SelectClearableCalendarsFragment.this.mContext.getText(R.string.delete_certain), SelectClearableCalendarsFragment.this.mClearEventsDialogListener);
                    alertDialogCreate.show();
                    SelectClearableCalendarsFragment.this.mAlertDialog = alertDialogCreate;
                    break;
                default:
                    LogUtil.e("Calendar", "Unexpected view called: " + view);
                    break;
            }
        }
    };
    private DialogInterface.OnClickListener mClearEventsDialogListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            LogUtil.d("Calendar", "Clear all events, to delete.");
            SelectClearableCalendarsFragment.this.dismissAlertDialog();
            int unused = SelectClearableCalendarsFragment.sDeleteToken = SelectClearableCalendarsFragment.this.mService.getNextToken();
            if (SelectClearableCalendarsFragment.this.mProgressDialog != null) {
                SelectClearableCalendarsFragment.this.mProgressDialog.show();
            }
            String selection = SelectClearableCalendarsFragment.this.getSelection("_id>0");
            LogUtil.i("Calendar", "Clear all events, start delete, selection=" + selection);
            SelectClearableCalendarsFragment.this.mService.startDelete(SelectClearableCalendarsFragment.sDeleteToken, null, CalendarContract.Events.CONTENT_URI, selection, null, 0L);
        }
    };
    ProgressDialog mProgressDialog = null;

    public SelectClearableCalendarsFragment() {
    }

    public SelectClearableCalendarsFragment(int i) {
        mCalendarItemLayout = i;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity;
        this.mService = new AsyncQueryService(activity) {
            @Override
            protected void onQueryComplete(int i, Object obj, Cursor cursor) {
                if (cursor == null) {
                    LogUtil.i("Calendar", "cursor is null, the provider process may be dead.");
                    return;
                }
                Cursor cursorUpdateAccountCheckStatus = SelectClearableCalendarsFragment.this.updateAccountCheckStatus(cursor);
                SelectClearableCalendarsFragment.this.mAdapter.changeCursor(cursorUpdateAccountCheckStatus);
                SelectClearableCalendarsFragment.this.mCursor = cursorUpdateAccountCheckStatus;
            }

            @Override
            protected void onDeleteComplete(int i, Object obj, int i2) {
                LogUtil.i("Calendar", "Clear all events,onDeleteComplete.  result(delete number)=" + i2);
                if (SelectClearableCalendarsFragment.this.mProgressDialog != null && SelectClearableCalendarsFragment.this.mProgressDialog.isShowing()) {
                    SelectClearableCalendarsFragment.this.mProgressDialog.cancel();
                    LogUtil.i("Calendar", "Cancel Progress dialog.");
                }
                if (SelectClearableCalendarsFragment.this.mToast == null) {
                    SelectClearableCalendarsFragment.this.mToast = Toast.makeText(SelectClearableCalendarsFragment.this.mContext, R.string.delete_completed, 0);
                }
                SelectClearableCalendarsFragment.this.mToast.show();
                AllInOneActivity.setClearEventsCompletedStatus(true);
                super.onDeleteComplete(i, obj, i2);
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        super.onCreateView(layoutInflater, viewGroup, bundle);
        if (mCalendarItemLayout == R.layout.mini_calendar_item) {
            LogUtil.i("Calendar", " set mCalendarItemLayout to be calendar_sync_item");
            mCalendarItemLayout = R.layout.calendar_sync_item;
        }
        this.mView = layoutInflater.inflate(R.layout.select_calendars_to_clear_fragment, (ViewGroup) null);
        this.mList = (ListView) this.mView.findViewById(R.id.list);
        if (Utils.getConfigBool(getActivity(), R.bool.multiple_pane_config)) {
            this.mList.setDivider(null);
            View viewFindViewById = this.mView.findViewById(R.id.manage_sync_set);
            if (viewFindViewById != null) {
                viewFindViewById.setVisibility(8);
            }
        }
        this.mBtnDelete = (Button) this.mView.findViewById(R.id.btn_ok);
        if (this.mBtnDelete != null) {
            this.mBtnDelete.setOnClickListener(this.mClickListener);
            this.mBtnDelete.setEnabled(this.mCalendarIds.size() > 0);
        }
        Button button = (Button) this.mView.findViewById(R.id.btn_cancel);
        if (button != null) {
            button.setOnClickListener(this.mClickListener);
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
    public void onCreate(Bundle bundle) {
        long[] longArray;
        super.onCreate(bundle);
        this.mProgressDialog = createProgressDialog();
        this.mCalendarIds.clear();
        if (bundle != null && (longArray = bundle.getLongArray("key_calendar_ids")) != null) {
            for (long j : longArray) {
                this.mCalendarIds.add(Long.valueOf(j));
            }
            LogUtil.i("Calendar", "restored calendar ids: " + this.mCalendarIds);
        }
        this.sQueryToken = this.mService.getNextToken();
        this.mService.startQuery(this.sQueryToken, null, CalendarContract.Calendars.CONTENT_URI, PROJECTION, "sync_events=?", SELECTION_ARGS, "account_name");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.mCalendarIds != null && !this.mCalendarIds.isEmpty()) {
            this.mCalendarIds.clear();
        }
        dismissAlertDialog();
        if (this.mProgressDialog != null) {
            this.mProgressDialog.dismiss();
            this.mProgressDialog = null;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (this.mCursor != null) {
            this.mAdapter.changeCursor(null);
            this.mCursor.close();
            this.mCursor = null;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
        if (this.mAdapter == null || this.mAdapter.getCount() <= i) {
            return;
        }
        saveCalendarId(i);
    }

    public void saveCalendarId(int i) {
        Log.d("Calendar", "Toggling calendar at " + i);
        long itemId = this.mAdapter.getItemId(i);
        int visible = this.mAdapter.getVisible(i) ^ 1;
        this.mAdapter.setVisible(i, visible);
        if (visible != 0) {
            this.mCalendarIds.add(Long.valueOf(itemId));
        } else if (this.mCalendarIds.contains(Long.valueOf(itemId))) {
            this.mCalendarIds.remove(Long.valueOf(itemId));
        }
        if (!this.mCalendarIds.isEmpty()) {
            this.mBtnDelete.setEnabled(true);
        } else {
            this.mBtnDelete.setEnabled(false);
        }
    }

    private String getSelection(String str) {
        String str2 = "";
        Iterator<Long> it = this.mCalendarIds.iterator();
        while (it.hasNext()) {
            str2 = str2 + " OR calendar_id=" + String.valueOf(it.next());
        }
        if (!TextUtils.isEmpty(str2)) {
            return str + " AND (" + str2.replaceFirst(" OR ", "") + ")";
        }
        return str;
    }

    private void dismissAlertDialog() {
        if (this.mAlertDialog != null) {
            this.mAlertDialog.dismiss();
        }
    }

    private ProgressDialog createProgressDialog() {
        ProgressDialog progressDialog = new ProgressDialog(this.mContext);
        progressDialog.setMessage(getString(R.string.wait_deleting_tip));
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        return progressDialog;
    }

    public Cursor updateAccountCheckStatus(Cursor cursor) {
        MatrixCursor matrixCursor = new MatrixCursor(cursor.getColumnNames());
        int columnCount = cursor.getColumnCount();
        String[] strArr = new String[columnCount];
        while (cursor.moveToNext()) {
            for (int i = 0; i < columnCount; i++) {
                strArr[i] = cursor.getString(i);
            }
            int columnIndex = cursor.getColumnIndex("_id");
            int columnIndex2 = cursor.getColumnIndex("visible");
            if (this.mCalendarIds.contains(Long.valueOf(cursor.getLong(columnIndex)))) {
                strArr[columnIndex2] = String.valueOf(1);
            } else {
                strArr[columnIndex2] = String.valueOf(0);
            }
            matrixCursor.addRow(strArr);
        }
        cursor.close();
        return matrixCursor;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        LogUtil.i("Calendar", "save calendar ids:" + this.mCalendarIds);
        int size = this.mCalendarIds.size();
        long[] jArr = new long[size];
        for (int i = 0; i < size; i++) {
            jArr[i] = this.mCalendarIds.get(i).longValue();
        }
        bundle.putLongArray("key_calendar_ids", jArr);
        super.onSaveInstanceState(bundle);
    }
}
