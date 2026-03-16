package com.android.server.am;

import android.R;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.view.WindowManager;
import com.android.server.input.InputManagerService;

final class AppErrorDialog extends BaseErrorDialog {
    static final long DISMISS_TIMEOUT = 300000;
    static final int FORCE_QUIT = 0;
    static final int FORCE_QUIT_AND_REPORT = 1;
    private final Handler mHandler;
    private final ProcessRecord mProc;
    private final AppErrorResult mResult;
    private final ActivityManagerService mService;

    public AppErrorDialog(Context context, ActivityManagerService service, AppErrorResult result, ProcessRecord app) {
        CharSequence name;
        super(context);
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                synchronized (AppErrorDialog.this.mService) {
                    if (AppErrorDialog.this.mProc != null && AppErrorDialog.this.mProc.crashDialog == AppErrorDialog.this) {
                        AppErrorDialog.this.mProc.crashDialog = null;
                    }
                }
                AppErrorDialog.this.mResult.set(msg.what);
                removeMessages(0);
                AppErrorDialog.this.dismiss();
            }
        };
        Resources res = context.getResources();
        this.mService = service;
        this.mProc = app;
        this.mResult = result;
        if (app.pkgList.size() == 1 && (name = context.getPackageManager().getApplicationLabel(app.info)) != null) {
            setMessage(res.getString(R.string.gpsNotifTitle, name.toString(), app.info.processName));
        } else {
            setMessage(res.getString(R.string.gpsVerifNo, app.processName.toString()));
        }
        setCancelable(false);
        setButton(-1, res.getText(R.string.granularity_label_line), this.mHandler.obtainMessage(0));
        if (app.errorReportReceiver != null) {
            setButton(-2, res.getText(R.string.granularity_label_link), this.mHandler.obtainMessage(1));
        }
        setTitle(res.getText(R.string.gpsNotifTicker));
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.setTitle("Application Error: " + app.info.processName);
        attrs.privateFlags |= InputManagerService.BTN_MOUSE;
        getWindow().setAttributes(attrs);
        if (app.persistent) {
            getWindow().setType(2010);
        }
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(0), DISMISS_TIMEOUT);
    }
}
