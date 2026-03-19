package com.android.server.am;

import android.R;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.BidiFormatter;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.server.input.InputManagerService;

final class AppNotRespondingDialog extends BaseErrorDialog implements View.OnClickListener {
    public static final int ALREADY_SHOWING = -2;
    public static final int CANT_SHOW = -1;
    static final int FORCE_CLOSE = 1;
    private static final String TAG = "AppNotRespondingDialog";
    static final int WAIT = 2;
    static final int WAIT_AND_REPORT = 3;
    private final Handler mHandler;
    private final ProcessRecord mProc;
    private final ActivityManagerService mService;

    public AppNotRespondingDialog(ActivityManagerService service, Context context, ProcessRecord app, ActivityRecord activity, boolean aboveSystem) {
        CharSequence name1;
        int resid;
        String string;
        super(context);
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Intent appErrorIntent = null;
                MetricsLogger.action(AppNotRespondingDialog.this.getContext(), 317, msg.what);
                switch (msg.what) {
                    case 1:
                        AppNotRespondingDialog.this.mService.killAppAtUsersRequest(AppNotRespondingDialog.this.mProc, AppNotRespondingDialog.this);
                        break;
                    case 2:
                    case 3:
                        synchronized (AppNotRespondingDialog.this.mService) {
                            try {
                                ActivityManagerService.boostPriorityForLockedSection();
                                ProcessRecord app2 = AppNotRespondingDialog.this.mProc;
                                if (msg.what == 3) {
                                    appErrorIntent = AppNotRespondingDialog.this.mService.mAppErrors.createAppErrorIntentLocked(app2, System.currentTimeMillis(), null);
                                }
                                app2.notResponding = false;
                                app2.notRespondingReport = null;
                                if (app2.anrDialog == AppNotRespondingDialog.this) {
                                    app2.anrDialog = null;
                                }
                                AppNotRespondingDialog.this.mService.mServices.scheduleServiceTimeoutLocked(app2);
                            } catch (Throwable th) {
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                throw th;
                            }
                        }
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        break;
                }
                if (appErrorIntent != null) {
                    try {
                        AppNotRespondingDialog.this.getContext().startActivity(appErrorIntent);
                    } catch (ActivityNotFoundException e) {
                        Slog.w(AppNotRespondingDialog.TAG, "bug report receiver dissappeared", e);
                    }
                }
                AppNotRespondingDialog.this.dismiss();
            }
        };
        this.mService = service;
        this.mProc = app;
        Resources res = context.getResources();
        setCancelable(false);
        if (activity != null) {
            name1 = activity.info.loadLabel(context.getPackageManager());
        } else {
            name1 = null;
        }
        CharSequence name2 = null;
        if (app.pkgList.size() == 1 && (name2 = context.getPackageManager().getApplicationLabel(app.info)) != null) {
            if (name1 != null) {
                resid = R.string.dynamic_mode_notification_summary;
            } else {
                name1 = name2;
                name2 = app.processName;
                resid = R.string.dynamic_mode_notification_title;
            }
        } else if (name1 != null) {
            name2 = app.processName;
            resid = R.string.dynamic_mode_notification_summary_v2;
        } else {
            name1 = app.processName;
            resid = R.string.dynamic_mode_notification_title_v2;
        }
        BidiFormatter bidi = BidiFormatter.getInstance();
        if (name2 != null) {
            string = res.getString(resid, bidi.unicodeWrap(name1.toString()), bidi.unicodeWrap(name2.toString()));
        } else {
            string = res.getString(resid, bidi.unicodeWrap(name1.toString()));
        }
        setTitle(string);
        if (aboveSystem) {
            getWindow().setType(2010);
        }
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.setTitle("Application Not Responding: " + app.info.processName);
        attrs.privateFlags = InputManagerService.BTN_MOUSE;
        getWindow().setAttributes(attrs);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout frame = (FrameLayout) findViewById(R.id.custom);
        Context context = getContext();
        LayoutInflater.from(context).inflate(R.layout.alert_dialog_leanback_button_panel_side, (ViewGroup) frame, true);
        TextView report = (TextView) findViewById(R.id.fontScale);
        report.setOnClickListener(this);
        boolean hasReceiver = this.mProc.errorReportReceiver != null;
        report.setVisibility(hasReceiver ? 0 : 8);
        TextView close = (TextView) findViewById(R.id.floating_toolbar_menu_item_image_button);
        close.setOnClickListener(this);
        TextView wait = (TextView) findViewById(R.id.floating_toolbar_menu_item_text);
        wait.setOnClickListener(this);
        findViewById(R.id.flagRequestAccessibilityButton).setVisibility(0);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.floating_toolbar_menu_item_image_button:
                this.mHandler.obtainMessage(1).sendToTarget();
                break;
            case R.id.floating_toolbar_menu_item_text:
                this.mHandler.obtainMessage(2).sendToTarget();
                break;
            case R.id.fontScale:
                this.mHandler.obtainMessage(3).sendToTarget();
                break;
        }
    }
}
