package com.android.contacts.activities;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import com.android.contacts.DealWithContactsService;
import com.android.contacts.MARK;
import com.android.contacts.R;
import com.android.contacts.common.model.account.SimAccountType;
import com.android.contacts.common.util.SimPhoneBookCommonUtil;
import java.lang.ref.WeakReference;
import java.util.HashMap;

public class SimContactsSelectActivity extends ListActivity implements DialogInterface.OnCancelListener {
    String accountName;
    String accountType;
    private Button cancel;
    private CheckBox checkAll;
    private Button doAction;
    private LinearLayout l;
    private TextView mEmptyText;
    private ImageView mImageView;
    private SelectAdapter mListAdapter;
    private ProgressDialog mProgressDialog;
    protected QueryHandler mQueryHandler;
    private int mSlotId;
    private TelephonyManager mTm;
    private int mode;
    private MsgReceiver msgReceiver;
    private TextView tv;
    static HashMap<Integer, MARK> IOMap = new HashMap<>();
    static HashMap<Integer, MARK> DMap = new HashMap<>();
    static final String[] RAW_CONTACTS_COLUMN_NAMES = {"_id", "contact_id", "account_type", "display_name", "account_name"};
    protected Cursor mCursor = null;
    protected int mInitialSelection = -1;
    Handler IOHandler = new Handler() {
        private int Count = 0;
        private MARK m;
        private int position;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (SimContactsSelectActivity.this.mCursor != null) {
                        this.position = 0;
                        Log.d("SimContactsSelectActivity", "*** mCursor count = " + SimContactsSelectActivity.this.mCursor.getCount());
                        Log.d("SimContactsSelectActivity", "*** mCursor  = " + SimContactsSelectActivity.this.mCursor);
                        while (SimContactsSelectActivity.this.mCursor.moveToPosition(this.position)) {
                            Log.d("SimContactsSelectActivity", "*** mCursor moveToNext = succes " + (this.position + 1));
                            switch (SimContactsSelectActivity.this.mode) {
                                case 0:
                                case 1:
                                    SimContactsSelectActivity.this.mCursor.getString(3);
                                    long rawId = SimContactsSelectActivity.this.mCursor.getLong(0);
                                    Log.d("SimContactsSelectActivity", "----> onListItemClick raw_contact_id == " + rawId);
                                    this.m = new MARK(rawId, true);
                                    SimContactsSelectActivity.IOMap.put(Integer.valueOf(this.position), this.m);
                                    this.position++;
                                    break;
                                case 2:
                                    int raw_id = SimContactsSelectActivity.this.mCursor.getInt(0);
                                    int contact_id = SimContactsSelectActivity.this.mCursor.getInt(1);
                                    this.m = new MARK();
                                    this.m.raw_id = raw_id;
                                    this.m.contact_id = contact_id;
                                    this.m.checked = true;
                                    SimContactsSelectActivity.DMap.put(Integer.valueOf(this.position), this.m);
                                    this.position++;
                                    break;
                            }
                        }
                        this.Count = this.position;
                        SimContactsSelectActivity.this.updateTitleWithCount(this.Count);
                        SimContactsSelectActivity.this.tv.setText(R.string.unSelect_all);
                        SimContactsSelectActivity.this.checkAllItem(true);
                    }
                    break;
                case 2:
                    if (SimContactsSelectActivity.this.mCursor != null) {
                        this.position = 0;
                        switch (SimContactsSelectActivity.this.mode) {
                            case 0:
                            case 1:
                                while (SimContactsSelectActivity.this.mCursor.moveToPosition(this.position)) {
                                    if (SimContactsSelectActivity.IOMap.containsKey(Integer.valueOf(this.position))) {
                                        SimContactsSelectActivity.IOMap.remove(Integer.valueOf(this.position));
                                    }
                                    this.position++;
                                }
                                break;
                            case 2:
                                while (SimContactsSelectActivity.this.mCursor.moveToPosition(this.position)) {
                                    if (SimContactsSelectActivity.DMap.containsKey(Integer.valueOf(this.position))) {
                                        SimContactsSelectActivity.DMap.remove(Integer.valueOf(this.position));
                                    }
                                    this.position++;
                                }
                                break;
                        }
                    }
                    this.Count = 0;
                    SimContactsSelectActivity.this.updateTitleWithCount(this.Count);
                    SimContactsSelectActivity.this.tv.setText(R.string.select_all);
                    SimContactsSelectActivity.this.checkAllItem(false);
                    break;
                case 3:
                    switch (SimContactsSelectActivity.this.mode) {
                        case 0:
                        case 1:
                            this.position = msg.arg1;
                            this.m = (MARK) msg.obj;
                            Log.d("SimContactsSelectActivity", "postion = " + this.position + " " + this.m);
                            SimContactsSelectActivity.IOMap.put(Integer.valueOf(this.position), this.m);
                            break;
                        case 2:
                            this.position = msg.arg1;
                            this.m = (MARK) msg.obj;
                            Log.d("SimContactsSelectActivity", "postion = " + this.position + " " + this.m);
                            SimContactsSelectActivity.DMap.put(Integer.valueOf(this.position), this.m);
                            break;
                    }
                    this.Count++;
                    SimContactsSelectActivity.this.updateTitleWithCount(this.Count);
                    break;
                case 4:
                    switch (SimContactsSelectActivity.this.mode) {
                        case 0:
                        case 1:
                            this.position = msg.arg1;
                            if (SimContactsSelectActivity.IOMap.containsKey(Integer.valueOf(this.position))) {
                                SimContactsSelectActivity.IOMap.remove(Integer.valueOf(this.position));
                            }
                            Log.d("SimContactsSelectActivity", "postion = " + this.position);
                            break;
                        case 2:
                            this.position = msg.arg1;
                            if (SimContactsSelectActivity.DMap.containsKey(Integer.valueOf(this.position))) {
                                SimContactsSelectActivity.DMap.remove(Integer.valueOf(this.position));
                            }
                            Log.d("SimContactsSelectActivity", "postion = " + this.position);
                            break;
                    }
                    this.Count--;
                    SimContactsSelectActivity.this.updateTitleWithCount(this.Count);
                    break;
            }
        }
    };
    Handler mHandle = new MyHandler(this);

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent intent = getIntent();
        if (intent != null) {
            this.accountName = intent.getStringExtra("account_name");
            this.accountType = intent.getStringExtra("account_type");
            this.accountName = this.accountName == null ? "Phone" : this.accountName;
            this.accountType = this.accountType == null ? "Phone" : this.accountType;
            this.mode = intent.getIntExtra("mode", -1);
            if (intent.hasExtra("slot")) {
                this.mSlotId = intent.getIntExtra("slot", -1);
            } else {
                this.mSlotId = 0;
            }
        }
        this.mTm = TelephonyManager.from(this);
        Log.d("SimContactsSelectActivity", "onCreate() accountName = " + this.accountName);
        Log.d("SimContactsSelectActivity", "onCreate() accountType = " + this.accountType);
        setContentView(R.layout.multiple_checkbox_main);
        this.mEmptyText = (TextView) findViewById(R.id.empty_list_textView);
        this.mImageView = (ImageView) findViewById(R.id.empty_list_imageView);
        this.mQueryHandler = new QueryHandler(getContentResolver());
        this.l = (LinearLayout) findViewById(R.id.select_list_linearLayout);
        this.tv = (TextView) findViewById(R.id.select_list_all_text);
        this.checkAll = (CheckBox) findViewById(R.id.select_list_all_item_checkBox);
        this.doAction = (Button) findViewById(R.id.common_softkey_left_button);
        this.cancel = (Button) findViewById(R.id.common_softkey_right_button);
        this.IOHandler.obtainMessage(2).sendToTarget();
        if (this.mode == 0) {
            if (this.mSlotId == 0) {
                if (this.mTm.isMultiSimEnabled()) {
                    setTitle(R.string.import_from_sim1);
                } else {
                    setTitle(R.string.import_from_sim);
                }
            } else {
                setTitle(R.string.import_from_sim2);
            }
            this.doAction.setText(R.string.doImport);
        } else if (this.mode == 1) {
            if (this.mSlotId == 0) {
                if (this.mTm.isMultiSimEnabled()) {
                    setTitle(R.string.export_to_sim1);
                } else {
                    setTitle(R.string.export_to_sim);
                }
            } else {
                setTitle(R.string.export_to_sim2);
            }
            this.doAction.setText(R.string.doExport);
        }
        IOMap.clear();
        DMap.clear();
        query();
        this.doAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SimContactsSelectActivity.this.mode != 1) {
                    SimContactsSelectActivity.this.importSelected();
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(SimContactsSelectActivity.this);
                builder.setMessage(R.string.warning_contacts_incomplete);
                builder.setTitle(R.string.warning_title);
                builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        SimContactsSelectActivity.this.importSelected();
                    }
                });
                if (builder != null) {
                    builder.show();
                }
            }
        });
        this.cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SimContactsSelectActivity.this.finish();
            }
        });
        this.l.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!SimContactsSelectActivity.this.checkAll.isChecked()) {
                    Message msg = SimContactsSelectActivity.this.IOHandler.obtainMessage(1);
                    msg.sendToTarget();
                    SimContactsSelectActivity.this.checkAll.setChecked(true);
                } else {
                    Message msg2 = SimContactsSelectActivity.this.IOHandler.obtainMessage(2);
                    msg2.sendToTarget();
                    SimContactsSelectActivity.this.checkAll.setChecked(false);
                }
            }
        });
        this.mProgressDialog = new ProgressDialog(this);
        this.msgReceiver = new MsgReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.android.contacts.PROGRESS");
        intentFilter.addAction("com.android.contacts.EXPORT_ERROR_OCCUR");
        intentFilter.addAction("com.android.contacts.DELETE_ERROR_OCCUR");
        intentFilter.addAction("com.android.contacts.DISMISS_DIALOG");
        registerReceiver(this.msgReceiver, intentFilter);
    }

    private void setTitleWithCount(int selectedCount, int resWithCount, int resWithoutCount) {
        if (selectedCount == 0) {
            setTitle(resWithoutCount);
        } else {
            setTitle(getResources().getString(resWithCount, Integer.valueOf(selectedCount)));
        }
    }

    private void updateTitleWithCount(int selectedCount) {
        if (this.mode == 0) {
            if (this.mSlotId == 0) {
                if (this.mTm.isMultiSimEnabled()) {
                    setTitleWithCount(selectedCount, R.string.import_from_sim1_number, R.string.import_from_sim1);
                    return;
                } else {
                    setTitleWithCount(selectedCount, R.string.import_from_sim_number, R.string.import_from_sim);
                    return;
                }
            }
            setTitleWithCount(selectedCount, R.string.import_from_sim2_number, R.string.import_from_sim2);
            return;
        }
        if (this.mode == 1) {
            if (this.mSlotId == 0) {
                if (this.mTm.isMultiSimEnabled()) {
                    setTitleWithCount(selectedCount, R.string.export_to_sim1_number, R.string.export_to_sim1);
                    return;
                } else {
                    setTitleWithCount(selectedCount, R.string.export_to_sim_number, R.string.export_to_sim);
                    return;
                }
            }
            setTitleWithCount(selectedCount, R.string.export_to_sim2_number, R.string.export_to_sim2);
        }
    }

    protected void checkAllItem(boolean checked) {
        ListView listView = getListView();
        int count = listView.getChildCount();
        for (int i = 0; i < count; i++) {
            Log.d("SimContactsSelectActivity", "checkAll Item i = " + i);
            View view = listView.getChildAt(i);
            CheckBox cb = (CheckBox) view.findViewById(R.id.multiple_checkbox);
            cb.setChecked(checked);
        }
    }

    private void importSelected() {
        switch (this.mode) {
            case 0:
                this.mProgressDialog.setTitle(R.string.importing_title);
                break;
            case 1:
                this.mProgressDialog.setTitle(R.string.exporting_title);
                break;
            case 2:
                this.mProgressDialog.setTitle(R.string.deleting_title);
                break;
        }
        this.mProgressDialog.setMessage(getString(R.string.waiting_message));
        this.mProgressDialog.setProgressStyle(1);
        this.mProgressDialog.setProgress(0);
        this.mProgressDialog.setCancelable(true);
        this.mProgressDialog.setCanceledOnTouchOutside(false);
        this.mProgressDialog.setOnCancelListener(this);
        if (IOMap != null && (this.mode == 1 || this.mode == 0)) {
            int select = 0;
            for (Integer key : IOMap.keySet()) {
                if (IOMap.get(key).checked) {
                    select++;
                }
            }
            Log.d("SimContactsSelectActivity", "---> select num = " + select);
            this.mProgressDialog.setMax(select);
        }
        if (DMap != null && this.mode == 2) {
            int select2 = 0;
            for (Integer key2 : DMap.keySet()) {
                if (DMap.get(key2).checked) {
                    select2++;
                }
            }
            Log.d("SimContactsSelectActivity", "---> select num = " + select2);
            this.mProgressDialog.setMax(select2);
        }
        this.mProgressDialog.show();
        Intent intent = new Intent(this, (Class<?>) DealWithContactsService.class);
        switch (this.mode) {
            case 0:
                intent.setAction("com.android.contacts.ACTION_IMPORT_FROM_SIM");
                intent.putExtra("android.contacts.extra.DATA", IOMap);
                intent.putExtra("android.contacts.extra.ACCOUNT_NAME", this.accountName);
                intent.putExtra("android.contacts.extra.ACCOUNT_TYPE", this.accountType);
                intent.putExtra("slot", this.mSlotId);
                startService(intent);
                log("start IMPORT_FROM_SIM");
                break;
            case 1:
                intent.setAction("com.android.contacts.ACTION_EXPORT_TO_SIM");
                intent.putExtra("android.contacts.extra.DATA", IOMap);
                intent.putExtra("slot", this.mSlotId);
                startService(intent);
                log("start EXPORT_TO_SIM");
                break;
            case 2:
                intent.setAction("com.android.contacts.ACTION_DELETE_CONTACTS");
                intent.putExtra("android.contacts.extra.DATA", DMap);
                startService(intent);
                log("start DEL_CONTACTS");
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.mProgressDialog != null && this.mProgressDialog.isShowing()) {
            this.mProgressDialog.dismiss();
        }
        IOMap.clear();
        DMap.clear();
        if (this.mCursor != null) {
            this.mCursor.deactivate();
            this.mCursor.close();
        }
        unregisterReceiver(this.msgReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    protected Uri resolveIntent() {
        Intent intent = getIntent();
        Uri uri = ContactsContract.RawContacts.CONTENT_URI;
        Uri uriDelete = Uri.withAppendedPath(ContactsContract.AUTHORITY_URI, "raw_contacts_distinct");
        if (intent.getData() == null) {
            switch (this.mode) {
                case 0:
                    intent.setData(getSimUri(this.mSlotId));
                    break;
                case 1:
                case 2:
                    intent.setData(uriDelete);
                    break;
                default:
                    intent.setData(null);
                    break;
            }
        } else {
            intent.setData(uri);
        }
        return intent.getData();
    }

    private class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor c) {
            SimContactsSelectActivity.this.log("onQueryComplete: cursor.count=" + c.getCount());
            SimContactsSelectActivity.this.mCursor = c;
            SimContactsSelectActivity.this.setAdapter();
            SimContactsSelectActivity.this.displayProgress(false);
        }

        @Override
        protected void onInsertComplete(int token, Object cookie, Uri uri) {
            SimContactsSelectActivity.this.log("onInsertComplete: requery");
            SimContactsSelectActivity.this.reQuery();
        }

        @Override
        protected void onUpdateComplete(int token, Object cookie, int result) {
            SimContactsSelectActivity.this.log("onUpdateComplete: requery");
            SimContactsSelectActivity.this.reQuery();
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            SimContactsSelectActivity.this.log("onDeleteComplete: requery");
            SimContactsSelectActivity.this.reQuery();
        }
    }

    private void reQuery() {
        query();
    }

    private void query() {
        Uri uri = resolveIntent();
        log("query: starting an async query");
        switch (this.mode) {
            case 1:
                this.mQueryHandler.startQuery(0, null, uri, RAW_CONTACTS_COLUMN_NAMES, "deleted != ? AND account_type != ? AND account_type != ?", new String[]{"1", "com.android.contact.sim", "com.android.contact.sim2"}, "account_name ASC, sort_key");
                break;
        }
        displayProgress(true);
    }

    private void displayProgress(boolean flag) {
        log("displayProgress: " + flag);
        this.mEmptyText.setText(flag ? R.string.simContacts_emptyLoading : R.string.simContacts_empty);
        this.mImageView.setVisibility(flag ? 8 : 0);
        getWindow().setFeatureInt(5, flag ? -1 : -2);
    }

    private void setAdapter() {
        if (this.mListAdapter == null) {
            this.mListAdapter = new SelectAdapter(this, R.layout.multiple_checkbox_main_row, this.mCursor);
            setListAdapter(this.mListAdapter);
        }
    }

    static class MyHandler extends Handler {
        private final WeakReference<SimContactsSelectActivity> mActivity;

        MyHandler(SimContactsSelectActivity act) {
            this.mActivity = new WeakReference<>(act);
        }

        @Override
        public void handleMessage(Message msg) {
            SimContactsSelectActivity parentActivity = this.mActivity.get();
            if (parentActivity == null) {
                parentActivity.log("MyHandler: Parent Activity is already GC!");
            } else {
                handleMessage(parentActivity, msg);
            }
        }

        private void handleMessage(final SimContactsSelectActivity act, Message msg) {
            String warningStr;
            switch (msg.what) {
                case 1:
                    if (act.mProgressDialog != null && act.mProgressDialog.isShowing()) {
                        act.mProgressDialog.dismiss();
                    }
                    act.finish();
                    break;
                case 2:
                    if (act.mProgressDialog != null && act.mProgressDialog.isShowing()) {
                        act.mProgressDialog.dismiss();
                    }
                    int sTotalCount = msg.arg1;
                    int sSimErrCount = msg.arg2;
                    AlertDialog.Builder builder = new AlertDialog.Builder(act);
                    if (SimPhoneBookCommonUtil.isOutOfSpace(act.mSlotId)) {
                        builder.setTitle(R.string.sim_card_out_of_space_title);
                        warningStr = act.getBaseContext().getString(R.string.sim_card_out_of_space_message, String.valueOf(sTotalCount - sSimErrCount), String.valueOf(sTotalCount));
                    } else {
                        builder.setTitle(R.string.error_occurs_title);
                        warningStr = act.getBaseContext().getString(R.string.export_error_message, String.valueOf(sSimErrCount), String.valueOf(sTotalCount));
                    }
                    builder.setMessage(warningStr);
                    builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            act.finish();
                        }
                    });
                    if (builder != null) {
                        builder.show();
                    }
                    break;
                case 3:
                    if (act.mProgressDialog != null && act.mProgressDialog.isShowing()) {
                        act.mProgressDialog.dismiss();
                    }
                    int sTotalCount2 = msg.arg1;
                    int sSimErrCount2 = msg.arg2;
                    AlertDialog.Builder builder2 = new AlertDialog.Builder(act);
                    String warningStr2 = act.getBaseContext().getString(R.string.batch_delete_error_message, String.valueOf(sTotalCount2 - sSimErrCount2), String.valueOf(sTotalCount2));
                    builder2.setTitle(R.string.error_occurs_title);
                    builder2.setMessage(warningStr2);
                    builder2.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            act.finish();
                        }
                    });
                    if (builder2 != null) {
                        builder2.show();
                    }
                    break;
            }
        }
    }

    private Uri getSimUri(int slotId) {
        String simAccountName = SimAccountType.getSimAccountName(slotId);
        String simAccountType = SimAccountType.getSimAccountType(slotId);
        Uri uri = ContactsContract.RawContacts.CONTENT_URI;
        Uri.Builder uriBuilder = uri.buildUpon();
        uriBuilder.appendQueryParameter("account_name", simAccountName);
        uriBuilder.appendQueryParameter("account_type", simAccountType);
        return uriBuilder.build();
    }

    protected void log(String msg) {
        Log.d("SimContactsSelectActivity", " " + msg);
    }

    private class SelectAdapter extends ResourceCursorAdapter {
        public SelectAdapter(Context context, int layout, Cursor c) {
            super(context, layout, c, false);
        }

        @Override
        public View newView(Context context, Cursor cur, ViewGroup parent) {
            LayoutInflater li = (LayoutInflater) SimContactsSelectActivity.this.getSystemService("layout_inflater");
            return li.inflate(R.layout.multiple_checkbox_main_row, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView tv1 = (TextView) view.findViewById(R.id.multiple_title);
            CheckBox cb = (CheckBox) view.findViewById(R.id.multiple_checkbox);
            switch (SimContactsSelectActivity.this.mode) {
                case 0:
                case 1:
                    tv1.setText("+ ");
                    String name = cursor.getString(3);
                    if (name == null) {
                        name = " (No name) ";
                    }
                    tv1.append(name);
                    if (SimContactsSelectActivity.IOMap.get(Integer.valueOf(cursor.getPosition())) != null && SimContactsSelectActivity.IOMap.get(Integer.valueOf(cursor.getPosition())).checked) {
                        cb.setChecked(true);
                    } else {
                        cb.setChecked(false);
                    }
                    break;
                case 2:
                    tv1.setText(" - ");
                    String name2 = cursor.getString(3);
                    if (name2 == null) {
                        name2 = " (No name) ";
                    }
                    tv1.append(name2);
                    String accountType = cursor.getString(2);
                    if (SimContactsSelectActivity.this.mTm.isMultiSimEnabled()) {
                        if (accountType != null && accountType.equals("com.android.contact.sim")) {
                            tv1.append(" [SIM1]");
                        } else if (accountType != null && accountType.equals("com.android.contact.sim2")) {
                            tv1.append(" [SIM2]");
                        }
                    } else if (accountType != null && accountType.equals("com.android.contact.sim")) {
                        tv1.append(" [SIM]");
                    }
                    if (SimContactsSelectActivity.DMap.get(Integer.valueOf(cursor.getPosition())) != null && SimContactsSelectActivity.DMap.get(Integer.valueOf(cursor.getPosition())).checked) {
                        cb.setChecked(true);
                    } else {
                        cb.setChecked(false);
                    }
                    break;
            }
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        CheckBox cb = (CheckBox) v.findViewById(R.id.multiple_checkbox);
        if (this.mCursor == null || !this.mCursor.moveToPosition(position)) {
            Log.e("SimContactsSelectActivity", "onListItemClick mCursor is null?" + (this.mCursor == null) + ", pos = " + position);
            return;
        }
        long raw_id = this.mCursor.getLong(0);
        long contact_id = this.mCursor.getLong(1);
        Message msg = null;
        MARK mark = new MARK();
        switch (this.mode) {
            case 0:
            case 1:
            case 2:
                Log.v("SimContactsSelectActivity", "----> onListItemClick raw_contact_id == " + raw_id);
                mark.raw_id = raw_id;
                mark.contact_id = contact_id;
                if (cb.isChecked()) {
                    cb.setChecked(false);
                    mark.checked = false;
                    msg = this.IOHandler.obtainMessage(4, position, 0, mark);
                } else {
                    cb.setChecked(true);
                    mark.checked = true;
                    msg = this.IOHandler.obtainMessage(3, position, 0, mark);
                }
                break;
        }
        if (msg != null) {
            msg.sendToTarget();
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        finish();
    }

    public class MsgReceiver extends BroadcastReceiver {
        public MsgReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            SimContactsSelectActivity.this.log("onReceive intent= " + intent);
            int slotId = intent.getIntExtra("slot", -1);
            if (!SubscriptionManager.isValidSlotId(slotId) || slotId == SimContactsSelectActivity.this.mSlotId) {
                if (intent.getAction().equals("com.android.contacts.PROGRESS")) {
                    int progress = intent.getIntExtra("android.contacts.extra.PROGRESS", 0);
                    SimContactsSelectActivity.this.mProgressDialog.incrementProgressBy(progress);
                    return;
                }
                if (intent.getAction().equals("com.android.contacts.EXPORT_ERROR_OCCUR")) {
                    int arg1 = intent.getIntExtra("android.contacts.extra.TOTAL_COUNT", 0);
                    int arg2 = intent.getIntExtra("android.contacts.extra.SIM_ERROR_COUNT", 0);
                    SimContactsSelectActivity.this.mHandle.obtainMessage(2, arg1, arg2).sendToTarget();
                    SimContactsSelectActivity.this.mProgressDialog.setCancelMessage(SimContactsSelectActivity.this.mHandle.obtainMessage(1));
                    return;
                }
                if (intent.getAction().equals("com.android.contacts.DELETE_ERROR_OCCUR")) {
                    int arg12 = intent.getIntExtra("android.contacts.extra.TOTAL_COUNT", 0);
                    int arg22 = intent.getIntExtra("android.contacts.extra.SIM_ERROR_COUNT", 0);
                    SimContactsSelectActivity.this.mHandle.obtainMessage(3, arg12, arg22).sendToTarget();
                    SimContactsSelectActivity.this.mProgressDialog.setCancelMessage(SimContactsSelectActivity.this.mHandle.obtainMessage(1));
                    return;
                }
                if (intent.getAction().equals("com.android.contacts.DISMISS_DIALOG")) {
                    SimContactsSelectActivity.this.mHandle.obtainMessage(1).sendToTarget();
                }
            }
        }
    }
}
