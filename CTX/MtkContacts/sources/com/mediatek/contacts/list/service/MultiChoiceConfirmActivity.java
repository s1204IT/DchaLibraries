package com.mediatek.contacts.list.service;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import com.android.contacts.R;
import com.android.contacts.model.account.BaseAccountType;
import com.mediatek.contacts.list.service.MultiChoiceHandlerListener;
import com.mediatek.contacts.list.service.MultiChoiceService;
import com.mediatek.contacts.util.Log;

public class MultiChoiceConfirmActivity extends Activity implements ServiceConnection {
    private String mAccountInfo;
    private int mJobId;
    private String mReportContent;
    private String mReportTitle;
    private int mType;
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.mediatek.intent.action.contacts.multichoice.process.finish".equals(intent.getAction())) {
                MultiChoiceConfirmActivity.this.finish();
            }
        }
    };
    private final CancelListener mCancelListener = new CancelListener();
    private Boolean mIsReportDialog = false;
    private MultiChoiceHandlerListener.ReportDialogInfo mDialogInfo = null;

    private class RequestCancelListener implements DialogInterface.OnClickListener {
        private RequestCancelListener() {
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            MultiChoiceConfirmActivity.this.bindService(new Intent(MultiChoiceConfirmActivity.this, (Class<?>) MultiChoiceService.class), MultiChoiceConfirmActivity.this, 1);
        }
    }

    private class CancelListener implements DialogInterface.OnCancelListener, DialogInterface.OnClickListener {
        private CancelListener() {
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            Log.d("MultiChoiceConfirmActivity", "[CancelListener] onClick: " + MultiChoiceConfirmActivity.this.mJobId);
            ((NotificationManager) MultiChoiceConfirmActivity.this.getSystemService("notification")).cancel("MultiChoiceServiceProgress", MultiChoiceConfirmActivity.this.mJobId);
            MultiChoiceConfirmActivity.this.finish();
        }

        @Override
        public void onCancel(DialogInterface dialogInterface) {
            Log.d("MultiChoiceConfirmActivity", "[CancelListener] onCancel: " + MultiChoiceConfirmActivity.this.mJobId);
            MultiChoiceConfirmActivity.this.finish();
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Log.i("MultiChoiceConfirmActivity", "[onCreate]savedInstanceState: " + bundle);
        if (bundle != null) {
            this.mIsReportDialog = Boolean.valueOf(bundle.getBoolean("report_dialog", false));
            if (this.mIsReportDialog.booleanValue()) {
                this.mDialogInfo = (MultiChoiceHandlerListener.ReportDialogInfo) bundle.getParcelable("report_dialog_info");
                this.mReportTitle = "";
                this.mReportContent = "";
                if (this.mDialogInfo != null) {
                    if (this.mDialogInfo.getmTitleId() != -1) {
                        this.mReportTitle = getString(this.mDialogInfo.getmTitleId(), new Object[]{Integer.valueOf(this.mDialogInfo.getmTotalNumber())});
                    }
                    if (this.mDialogInfo.getmContentId() != -1) {
                        this.mReportContent = getString(this.mDialogInfo.getmContentId(), new Object[]{Integer.valueOf(this.mDialogInfo.getmSucceededNumber()), Integer.valueOf(this.mDialogInfo.getmFailedNumber())});
                        return;
                    }
                    return;
                }
                return;
            }
            this.mJobId = bundle.getInt("job_id", -1);
            this.mAccountInfo = bundle.getString("account_info");
            this.mType = bundle.getInt(BaseAccountType.Attr.TYPE, 0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        this.mIsReportDialog = Boolean.valueOf(intent.getBooleanExtra("report_dialog", false));
        Log.i("MultiChoiceConfirmActivity", "[onResume]mReportDialog : " + this.mIsReportDialog);
        if (this.mIsReportDialog.booleanValue()) {
            this.mDialogInfo = (MultiChoiceHandlerListener.ReportDialogInfo) intent.getParcelableExtra("report_dialog_info");
            this.mReportTitle = "";
            this.mReportContent = "";
            if (this.mDialogInfo != null) {
                if (this.mDialogInfo.getmTitleId() != -1) {
                    this.mReportTitle = getString(this.mDialogInfo.getmTitleId(), new Object[]{Integer.valueOf(this.mDialogInfo.getmTotalNumber())});
                }
                if (this.mDialogInfo.getmContentId() != -1) {
                    this.mReportContent = getString(this.mDialogInfo.getmContentId(), new Object[]{Integer.valueOf(this.mDialogInfo.getmSucceededNumber()), Integer.valueOf(this.mDialogInfo.getmFailedNumber())});
                }
                this.mJobId = this.mDialogInfo.getmJobId();
                Log.i("MultiChoiceConfirmActivity", "[onResume]mJobId : " + this.mJobId);
            }
        } else {
            this.mJobId = intent.getIntExtra("job_id", -1);
            this.mAccountInfo = intent.getStringExtra("account_info");
            this.mType = intent.getIntExtra(BaseAccountType.Attr.TYPE, 0);
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.mediatek.intent.action.contacts.multichoice.process.finish");
        registerReceiver(this.mIntentReceiver, intentFilter);
        Log.i("MultiChoiceConfirmActivity", "[onResume]mReportTitle : " + this.mReportTitle + " | mReportContent : " + this.mReportContent);
        if (this.mIsReportDialog.booleanValue()) {
            showDialog(R.id.multichoice_report_dialog);
        } else {
            showDialog(R.id.multichoice_confirm_dialog);
        }
    }

    @Override
    protected Dialog onCreateDialog(int i, Bundle bundle) {
        String string;
        String string2;
        Log.i("MultiChoiceConfirmActivity", "[onCreateDialog]id : " + i);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (i == R.id.multichoice_confirm_dialog) {
            if (this.mType == 2) {
                string = getString(R.string.multichoice_confirmation_title_delete);
                string2 = getString(R.string.multichoice_confirmation_message_delete);
            } else {
                string = getString(R.string.multichoice_confirmation_title_copy);
                string2 = getString(R.string.multichoice_confirmation_message_copy);
            }
            builder.setTitle(string).setMessage(string2).setPositiveButton(android.R.string.ok, new RequestCancelListener()).setOnCancelListener(this.mCancelListener).setNegativeButton(android.R.string.cancel, this.mCancelListener);
            return builder.create();
        }
        if (i == R.id.multichoice_report_dialog) {
            builder.setTitle(this.mReportTitle).setMessage(this.mReportContent).setPositiveButton(android.R.string.ok, this.mCancelListener).setOnCancelListener(this.mCancelListener).setNegativeButton(android.R.string.cancel, this.mCancelListener);
            return builder.create();
        }
        Log.w("MultiChoiceConfirmActivity", "[onCreateDialog]Unknown dialog id: " + i);
        return super.onCreateDialog(i, bundle);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        try {
            ((MultiChoiceService.MyBinder) iBinder).getService().handleCancelRequest(new MultiChoiceCancelRequest(this.mJobId));
            unbindService(this);
            finish();
        } catch (Throwable th) {
            unbindService(this);
            throw th;
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(this.mIntentReceiver);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        Log.i("MultiChoiceConfirmActivity", "[onSaveInstanceState]");
        bundle.putBoolean("report_dialog", this.mIsReportDialog.booleanValue());
        bundle.putInt("job_id", this.mJobId);
        bundle.putString("account_info", this.mAccountInfo);
        bundle.putInt(BaseAccountType.Attr.TYPE, this.mType);
        if (this.mIsReportDialog.booleanValue()) {
            bundle.putParcelable("report_dialog_info", this.mDialogInfo);
        }
        super.onSaveInstanceState(bundle);
    }

    @Override
    protected void onPrepareDialog(int i, Dialog dialog, Bundle bundle) {
        Log.i("MultiChoiceConfirmActivity", "[onPrepareDialog]mReportContent : " + this.mReportContent + " | mReportTitle : " + this.mReportTitle + "|id :" + i);
        super.onPrepareDialog(i, dialog, bundle);
        if (i == R.id.multichoice_report_dialog) {
            AlertDialog alertDialog = (AlertDialog) dialog;
            alertDialog.setMessage(this.mReportContent);
            alertDialog.setTitle(this.mReportTitle);
        }
    }
}
