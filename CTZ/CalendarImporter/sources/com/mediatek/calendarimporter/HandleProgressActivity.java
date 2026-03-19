package com.mediatek.calendarimporter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.mediatek.calendarimporter.BindServiceHelper;
import com.mediatek.calendarimporter.service.ImportProcessor;
import com.mediatek.calendarimporter.service.ProcessorMsgType;
import com.mediatek.calendarimporter.service.VCalService;
import com.mediatek.calendarimporter.utils.LogUtils;
import com.mediatek.calendarimporter.utils.Utils;
import java.util.ArrayList;

public class HandleProgressActivity extends Activity implements DialogInterface.OnCancelListener, DialogInterface.OnClickListener, BindServiceHelper.ServiceConnectedOperation {
    private static final String ACCOUNT_NAME = "TargetAccountName";
    protected static final String BUNDLE_KEY_START_MILLIS = "key_start_millis";
    private static final String DATA_URI = "DataUri";
    private static final int ID_DIALOG_NO_CALENDAR_ALERT = 1;
    private static final int ID_DIALOG_PROGRESS_BAR = 2;
    private static final String TAG = "HandleProgressActivity";
    private String mAccountName;
    private AlertDialog mAlertDialog;
    private Uri mDataUri;
    private boolean mFirstEnter;
    private Handler mHandler;
    private ImportProcessor mProcessor;
    private ProgressDialog mProgressDialog;
    private VCalService mService;
    private BindServiceHelper mServiceHelper;
    private boolean mIgnoreMessage = false;
    private ListView mAccountList = null;
    private final DialogInterface.OnClickListener mDialogListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            switch (i) {
                case -2:
                    if (HandleProgressActivity.this.mService != null) {
                        HandleProgressActivity.this.mService.tryCancelProcessor(HandleProgressActivity.this.mProcessor);
                    }
                    break;
                case ProcessorMsgType.PROCESSOR_EXCEPTION:
                    HandleProgressActivity.this.addParseRequest();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        requestWindowFeature(1);
        this.mDataUri = getIntent().getData();
        this.mFirstEnter = true;
        if (Utils.hasExchangeOrGoogleAccount(this)) {
            this.mServiceHelper = new BindServiceHelper(this);
            this.mServiceHelper.onBindService();
            showAccountListView();
        } else {
            LogUtils.e(TAG, "onCreate, should not be created when no account exists.");
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean z) {
        LogUtils.d(TAG, "onWindowFocusChanged() entry ");
        if (!z) {
            this.mFirstEnter = false;
            LogUtils.d(TAG, "onWindowFocusChanged() return ");
            finish();
            return;
        }
        if (Utils.hasExchangeOrGoogleAccount(this)) {
            showAccountListView();
            LogUtils.d(TAG, "onWindowFocusChanged() showAccountListView ");
        } else if (!this.mFirstEnter) {
            this.mHandler = null;
            LogUtils.d(TAG, "onWindowFocusChanged() finish ");
            finish();
        }
        super.onWindowFocusChanged(z);
    }

    private void showAccountListView() {
        setContentView(R.layout.account);
        int themeMainColor = Utils.getThemeMainColor(this, 17170450);
        if (themeMainColor != 17170450) {
            ((TextView) findViewById(R.id.account_title)).setTextColor(themeMainColor);
            findViewById(R.id.account_devide_line).setBackgroundColor(themeMainColor);
        }
        this.mAccountList = (ListView) findViewById(R.id.account_list);
        this.mAccountList.setAdapter((ListAdapter) new ArrayAdapter(this, R.layout.list_adapter, getAccount()));
        setTitleColor(-7829368);
        this.mAccountList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
                HandleProgressActivity.this.mAccountName = ((TextView) view).getText().toString();
                LogUtils.d(HandleProgressActivity.TAG, "showAccountListView() Select = " + ("account_name=\"" + HandleProgressActivity.this.mAccountName + "\""));
                HandleProgressActivity.this.addParseRequest();
                HandleProgressActivity.this.mAccountList.setEnabled(false);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        bundle.putParcelable(DATA_URI, this.mDataUri);
        bundle.putString(ACCOUNT_NAME, this.mAccountName);
        super.onSaveInstanceState(bundle);
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        this.mDataUri = (Uri) bundle.getParcelable(DATA_URI);
        this.mAccountName = bundle.getString(ACCOUNT_NAME);
    }

    private String[] getAccount() {
        Account[] accounts = AccountManager.get(getApplicationContext()).getAccounts();
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < accounts.length; i++) {
            if (Utils.isExchangeOrGoogleAccount(accounts[i])) {
                arrayList.add(accounts[i].name);
            }
        }
        return (String[]) arrayList.toArray(new String[arrayList.size()]);
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        finish();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        finish();
    }

    @Override
    protected Dialog onCreateDialog(int i) {
        LogUtils.d(TAG, "onCreateDialog,id=" + i);
        if (1 == i) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.no_syncable_calendars).setIcon(R.drawable.ic_dialog_alert_holo_light).setMessage(R.string.no_calendars_found).setPositiveButton(R.string.retry, this.mDialogListener).setNegativeButton(R.string.give_up, this.mDialogListener);
            this.mAlertDialog = builder.create();
            return this.mAlertDialog;
        }
        if (2 == i) {
            this.mProgressDialog = new ProgressDialog(this);
            this.mProgressDialog.setTitle(R.string.import_title);
            this.mProgressDialog.setProgressStyle(1);
            this.mProgressDialog.setCancelable(false);
            this.mProgressDialog.setIndeterminate(false);
            this.mProgressDialog.setButton(getText(R.string.give_up), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i2) {
                    if (HandleProgressActivity.this.mService != null && HandleProgressActivity.this.mProcessor != null) {
                        HandleProgressActivity.this.mService.tryCancelProcessor(HandleProgressActivity.this.mProcessor);
                        HandleProgressActivity.this.mProgressDialog.dismiss();
                    }
                }
            });
            return this.mProgressDialog;
        }
        return null;
    }

    private void addParseRequest() {
        if (this.mService != null && this.mHandler != null && this.mAccountName != null && this.mDataUri != null) {
            LogUtils.d(TAG, "addParseRequest. AccountName = " + this.mAccountName);
            this.mProcessor = new ImportProcessor(this, this.mAccountName, this.mHandler, this.mDataUri);
            this.mService.tryExecuteProcessor(this.mProcessor);
        }
    }

    @Override
    protected void onDestroy() {
        LogUtils.d(TAG, "onDestroy() + mService:" + this.mService);
        if (this.mServiceHelper != null) {
            this.mServiceHelper.unBindService();
        }
        this.mIgnoreMessage = true;
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        LogUtils.d(TAG, "onBackPressed() + mService:" + this.mService);
        if (this.mService != null) {
            this.mService.tryCancelProcessor(this.mProcessor);
        }
        super.onBackPressed();
    }

    @Override
    public void serviceConnected(VCalService vCalService) {
        this.mService = vCalService;
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (HandleProgressActivity.this.mIgnoreMessage) {
                    LogUtils.i(HandleProgressActivity.TAG, "activity destroyed no need to handle response");
                }
                switch (message.what) {
                    case ProcessorMsgType.PROCESSOR_EXCEPTION:
                        LogUtils.i(HandleProgressActivity.TAG, "serviceConnected. ProcessorMsgType:PROCESSOR_EXCEPTION. type = " + message.arg2);
                        if (1 == message.arg2) {
                            Toast.makeText(HandleProgressActivity.this, R.string.no_calendars_found, 1).show();
                            HandleProgressActivity.this.finish();
                        } else {
                            Toast.makeText(HandleProgressActivity.this, R.string.import_vcs_failed, 0).show();
                            HandleProgressActivity.this.finish();
                        }
                        break;
                    case 1:
                        if (HandleProgressActivity.this.mProgressDialog != null) {
                            HandleProgressActivity.this.mProgressDialog.dismiss();
                        }
                        LogUtils.i(HandleProgressActivity.TAG, "serviceConnected,ProcessorMsgType:PROCESSOR_FINISH. Start result Activity.");
                        Intent intent = new Intent();
                        intent.setClass(HandleProgressActivity.this, ShowHandleResultActivity.class);
                        intent.putExtra("SucceedCnt", message.arg1);
                        intent.putExtra("totalCnt", message.arg2);
                        intent.putExtra("accountName", HandleProgressActivity.this.mAccountName);
                        long j = ((Bundle) message.obj).getLong(HandleProgressActivity.BUNDLE_KEY_START_MILLIS, -1L);
                        LogUtils.d(HandleProgressActivity.TAG, "serviceConnected,ProcessorMsgType:PROCESSOR_FINISH. DtStart = " + j);
                        intent.putExtra("eventStartTime", j);
                        HandleProgressActivity.this.startActivity(intent);
                        HandleProgressActivity.this.finish();
                        break;
                }
            }
        };
    }

    @Override
    public void serviceUnConnected() {
        this.mService = null;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (this.mService == null) {
            this.mServiceHelper = new BindServiceHelper(this);
            this.mServiceHelper.onBindService();
        }
        this.mDataUri = intent.getData();
        if (this.mAlertDialog != null) {
            this.mAlertDialog.dismiss();
        }
        if (this.mProgressDialog != null) {
            this.mProgressDialog.dismiss();
            if (this.mService != null) {
                this.mService.tryCancelProcessor(this.mProcessor);
            }
        }
        if (Utils.hasExchangeOrGoogleAccount(this)) {
            showAccountListView();
        } else {
            LogUtils.e(TAG, "onNewIntent, should not continue when no account exists.");
            finish();
        }
    }
}
