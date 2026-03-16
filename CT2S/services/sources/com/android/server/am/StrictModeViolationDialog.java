package com.android.server.am;

import android.R;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import com.android.server.pm.PackageManagerService;

final class StrictModeViolationDialog extends BaseErrorDialog {
    static final int ACTION_OK = 0;
    static final int ACTION_OK_AND_REPORT = 1;
    static final long DISMISS_TIMEOUT = 60000;
    private static final String TAG = "StrictModeViolationDialog";
    private final Handler mHandler;
    private final ProcessRecord mProc;
    private final AppErrorResult mResult;
    private final ActivityManagerService mService;

    public StrictModeViolationDialog(Context context, ActivityManagerService service, AppErrorResult result, ProcessRecord app) {
        CharSequence name;
        super(context);
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                synchronized (StrictModeViolationDialog.this.mService) {
                    if (StrictModeViolationDialog.this.mProc != null && StrictModeViolationDialog.this.mProc.crashDialog == StrictModeViolationDialog.this) {
                        StrictModeViolationDialog.this.mProc.crashDialog = null;
                    }
                }
                StrictModeViolationDialog.this.mResult.set(msg.what);
                StrictModeViolationDialog.this.dismiss();
            }
        };
        Resources res = context.getResources();
        this.mService = service;
        this.mProc = app;
        this.mResult = result;
        if (app.pkgList.size() == 1 && (name = context.getPackageManager().getApplicationLabel(app.info)) != null) {
            setMessage(res.getString(R.string.hearing_device_notification_settings_button, name.toString(), app.info.processName));
        } else {
            setMessage(res.getString(R.string.hearing_device_notification_switch_button, app.processName.toString()));
        }
        setCancelable(false);
        setButton(-1, res.getText(R.string.indeterminate_progress_39), this.mHandler.obtainMessage(0));
        if (app.errorReportReceiver != null) {
            setButton(-2, res.getText(R.string.granularity_label_link), this.mHandler.obtainMessage(1));
        }
        setTitle(res.getText(R.string.gpsNotifTicker));
        getWindow().addPrivateFlags(PackageManagerService.DumpState.DUMP_VERIFIERS);
        getWindow().setTitle("Strict Mode Violation: " + app.info.processName);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(0), DISMISS_TIMEOUT);
    }
}
