package com.android.server.am;

import android.R;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.util.Slog;
import android.view.WindowManager;
import com.android.server.input.InputManagerService;

final class AppNotRespondingDialog extends BaseErrorDialog {
    static final int FORCE_CLOSE = 1;
    private static final String TAG = "AppNotRespondingDialog";
    static final int WAIT = 2;
    static final int WAIT_AND_REPORT = 3;
    private final Handler mHandler;
    private final ProcessRecord mProc;
    private final ActivityManagerService mService;

    public AppNotRespondingDialog(ActivityManagerService service, Context context, ProcessRecord app, ActivityRecord activity, boolean aboveSystem) {
        int resid;
        super(context);
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Intent appErrorIntent = null;
                switch (msg.what) {
                    case 1:
                        AppNotRespondingDialog.this.mService.killAppAtUsersRequest(AppNotRespondingDialog.this.mProc, AppNotRespondingDialog.this);
                        break;
                    case 2:
                    case 3:
                        synchronized (AppNotRespondingDialog.this.mService) {
                            ProcessRecord app2 = AppNotRespondingDialog.this.mProc;
                            if (msg.what == 3) {
                                appErrorIntent = AppNotRespondingDialog.this.mService.createAppErrorIntentLocked(app2, System.currentTimeMillis(), null);
                            }
                            app2.notResponding = false;
                            app2.notRespondingReport = null;
                            if (app2.anrDialog == AppNotRespondingDialog.this) {
                                app2.anrDialog = null;
                            }
                            AppNotRespondingDialog.this.mService.mServices.scheduleServiceTimeoutLocked(app2);
                            break;
                        }
                        break;
                }
                if (appErrorIntent != null) {
                    try {
                        AppNotRespondingDialog.this.getContext().startActivity(appErrorIntent);
                    } catch (ActivityNotFoundException e) {
                        Slog.w(AppNotRespondingDialog.TAG, "bug report receiver dissappeared", e);
                    }
                }
            }
        };
        this.mService = service;
        this.mProc = app;
        Resources res = context.getResources();
        setCancelable(false);
        CharSequence name1 = activity != null ? activity.info.loadLabel(context.getPackageManager()) : null;
        CharSequence name2 = null;
        if (app.pkgList.size() == 1 && (name2 = context.getPackageManager().getApplicationLabel(app.info)) != null) {
            if (name1 != null) {
                resid = R.string.grant_credentials_permission_message_footer;
            } else {
                name1 = name2;
                name2 = app.processName;
                resid = R.string.grant_permissions_header_text;
            }
        } else if (name1 != null) {
            name2 = app.processName;
            resid = R.string.grant_credentials_permission_message_header;
        } else {
            name1 = app.processName;
            resid = R.string.granularity_label_character;
        }
        setMessage(name2 != null ? res.getString(resid, name1.toString(), name2.toString()) : res.getString(resid, name1.toString()));
        setButton(-1, res.getText(R.string.granularity_label_line), this.mHandler.obtainMessage(1));
        setButton(-2, res.getText(R.string.granularity_label_word), this.mHandler.obtainMessage(2));
        if (app.errorReportReceiver != null) {
            setButton(-3, res.getText(R.string.granularity_label_link), this.mHandler.obtainMessage(3));
        }
        setTitle(res.getText(R.string.gpsVerifYes));
        if (aboveSystem) {
            getWindow().setType(2010);
        }
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.setTitle("Application Not Responding: " + app.info.processName);
        attrs.privateFlags = InputManagerService.BTN_MOUSE;
        getWindow().setAttributes(attrs);
    }

    @Override
    public void onStop() {
    }
}
