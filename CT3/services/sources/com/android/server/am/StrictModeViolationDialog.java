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

    public StrictModeViolationDialog(Context context, ActivityManagerService service, AppErrorResult result, ProcessRecord app) {
        CharSequence name;
        super(context);
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
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
                StrictModeViolationDialog.this.mResult.set(msg.what);
                StrictModeViolationDialog.this.dismiss();
            }
        };
        Resources res = context.getResources();
        this.mService = service;
        this.mProc = app;
        this.mResult = result;
        if (app.pkgList.size() == 1 && (name = context.getPackageManager().getApplicationLabel(app.info)) != null) {
            setMessage(res.getString(R.string.enablePin, name.toString(), app.info.processName));
        } else {
            setMessage(res.getString(R.string.enable_explore_by_touch_warning_message, app.processName.toString()));
        }
        setCancelable(false);
        setButton(-1, res.getText(R.string.face_error_vendor_unknown), this.mHandler.obtainMessage(0));
        if (app.errorReportReceiver != null) {
            setButton(-2, res.getText(R.string.edit_accessibility_shortcut_menu_button), this.mHandler.obtainMessage(1));
        }
        getWindow().addPrivateFlags(256);
        getWindow().setTitle("Strict Mode Violation: " + app.info.processName);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(0), DISMISS_TIMEOUT);
    }
}
