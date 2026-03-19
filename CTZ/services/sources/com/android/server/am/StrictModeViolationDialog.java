package com.android.server.am;

import android.R;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;

final class StrictModeViolationDialog extends BaseErrorDialog {
    static final int ACTION_OK = 0;
    static final int ACTION_OK_AND_REPORT = 1;
    static final long DISMISS_TIMEOUT = 60000;
    private static final String TAG = "StrictModeViolationDialog";
    private final Handler mHandler;
    private final ProcessRecord mProc;
    private final AppErrorResult mResult;
    private final ActivityManagerService mService;

    public StrictModeViolationDialog(Context context, ActivityManagerService activityManagerService, AppErrorResult appErrorResult, ProcessRecord processRecord) {
        CharSequence applicationLabel;
        super(context);
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                synchronized (StrictModeViolationDialog.this.mService) {
                    try {
                        ActivityManagerService.boostPriorityForLockedSection();
                        if (StrictModeViolationDialog.this.mProc != null && StrictModeViolationDialog.this.mProc.crashDialog == StrictModeViolationDialog.this) {
                            StrictModeViolationDialog.this.mProc.crashDialog = null;
                        }
                    } catch (Throwable th) {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
                StrictModeViolationDialog.this.mResult.set(message.what);
                StrictModeViolationDialog.this.dismiss();
            }
        };
        Resources resources = context.getResources();
        this.mService = activityManagerService;
        this.mProc = processRecord;
        this.mResult = appErrorResult;
        if (processRecord.pkgList.size() == 1 && (applicationLabel = context.getPackageManager().getApplicationLabel(processRecord.info)) != null) {
            setMessage(resources.getString(R.string.mediasize_iso_a8, applicationLabel.toString(), processRecord.info.processName));
        } else {
            setMessage(resources.getString(R.string.mediasize_iso_a9, processRecord.processName.toString()));
        }
        setCancelable(false);
        setButton(-1, resources.getText(R.string.bugreport_option_interactive_title), this.mHandler.obtainMessage(0));
        if (processRecord.errorReportReceiver != null) {
            setButton(-2, resources.getText(R.string.kg_sim_unlock_progress_dialog_message), this.mHandler.obtainMessage(1));
        }
        getWindow().addPrivateFlags(256);
        getWindow().setTitle("Strict Mode Violation: " + processRecord.info.processName);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(0), 60000L);
    }
}
