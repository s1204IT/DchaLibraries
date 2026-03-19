package com.android.server.am;

import android.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.BidiFormatter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.server.input.InputManagerService;

public final class AppErrorDialog extends BaseErrorDialog implements View.OnClickListener {
    static final int CANCEL = 7;
    static final long DISMISS_TIMEOUT = 300000;
    static final int FORCE_QUIT = 1;
    static final int FORCE_QUIT_AND_REPORT = 2;
    static final int MUTE = 5;
    static final int RESTART = 3;
    static final int TIMEOUT = 6;
    private final boolean mForeground;
    private final Handler mHandler;
    private CharSequence mName;
    private final ProcessRecord mProc;
    private final BroadcastReceiver mReceiver;
    private final boolean mRepeating;
    private final AppErrorResult mResult;
    private final ActivityManagerService mService;
    static int CANT_SHOW = -1;
    static int BACKGROUND_USER = -2;
    static int ALREADY_SHOWING = -3;

    public static class Data {
        public String exceptionMsg;
        ProcessRecord proc;
        boolean repeating;
        AppErrorResult result;
        TaskRecord task;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return super.dispatchKeyEvent(event);
    }

    public AppErrorDialog(Context context, ActivityManagerService service, Data data) {
        super(context);
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                int result = msg.what;
                synchronized (AppErrorDialog.this.mService) {
                    try {
                        ActivityManagerService.boostPriorityForLockedSection();
                        if (AppErrorDialog.this.mProc != null && AppErrorDialog.this.mProc.crashDialog == AppErrorDialog.this) {
                            AppErrorDialog.this.mProc.crashDialog = null;
                        }
                    } catch (Throwable th) {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
                AppErrorDialog.this.mResult.set(result);
                removeMessages(6);
                AppErrorDialog.this.dismiss();
            }
        };
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (!"android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                    return;
                }
                AppErrorDialog.this.cancel();
            }
        };
        Resources res = context.getResources();
        this.mService = service;
        this.mProc = data.proc;
        this.mResult = data.result;
        this.mRepeating = data.repeating;
        this.mForeground = data.task != null;
        BidiFormatter bidi = BidiFormatter.getInstance();
        if (this.mProc.pkgList.size() == 1) {
            CharSequence applicationLabel = context.getPackageManager().getApplicationLabel(this.mProc.info);
            this.mName = applicationLabel;
            if (applicationLabel != null) {
                setTitle(res.getString(this.mRepeating ? R.string.duration_years_medium : R.string.duration_minutes_shortest_future, bidi.unicodeWrap(this.mName.toString()), bidi.unicodeWrap(this.mProc.info.processName)));
            } else {
                this.mName = this.mProc.processName;
                setTitle(res.getString(this.mRepeating ? R.string.duration_years_medium_future : R.string.duration_minutes_shortest_past, bidi.unicodeWrap(this.mName.toString())));
            }
        }
        setCancelable(true);
        setCancelMessage(this.mHandler.obtainMessage(7));
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.setTitle("Application Error: " + this.mProc.info.processName);
        attrs.privateFlags |= InputManagerService.BTN_MOUSE;
        getWindow().setAttributes(attrs);
        if (this.mProc.persistent) {
            getWindow().setType(2010);
        }
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(6), 300000L);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout frame = (FrameLayout) findViewById(R.id.custom);
        Context context = getContext();
        LayoutInflater.from(context).inflate(R.layout.alert_dialog_material, (ViewGroup) frame, true);
        boolean z = !this.mRepeating ? this.mForeground : false;
        boolean hasReceiver = this.mProc.errorReportReceiver != null;
        TextView restart = (TextView) findViewById(R.id.fontWeightAdjustment);
        restart.setOnClickListener(this);
        restart.setVisibility(z ? 0 : 8);
        TextView report = (TextView) findViewById(R.id.fontScale);
        report.setOnClickListener(this);
        report.setVisibility(hasReceiver ? 0 : 8);
        TextView close = (TextView) findViewById(R.id.floating_toolbar_menu_item_image_button);
        close.setVisibility(!z ? 0 : 8);
        close.setOnClickListener(this);
        boolean showMute = (ActivityManagerService.IS_USER_BUILD || Settings.Global.getInt(context.getContentResolver(), "development_settings_enabled", 0) == 0) ? false : true;
        TextView mute = (TextView) findViewById(R.id.four);
        mute.setOnClickListener(this);
        mute.setVisibility(showMute ? 0 : 8);
        findViewById(R.id.flagRequestAccessibilityButton).setVisibility(0);
    }

    @Override
    public void onStart() {
        super.onStart();
        getContext().registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
    }

    @Override
    protected void onStop() {
        super.onStop();
        getContext().unregisterReceiver(this.mReceiver);
    }

    @Override
    public void dismiss() {
        if (!this.mResult.mHasResult) {
            this.mResult.set(1);
        }
        super.dismiss();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.floating_toolbar_menu_item_image_button:
                this.mHandler.obtainMessage(1).sendToTarget();
                break;
            case R.id.fontScale:
                this.mHandler.obtainMessage(2).sendToTarget();
                break;
            case R.id.fontWeightAdjustment:
                this.mHandler.obtainMessage(3).sendToTarget();
                break;
            case R.id.four:
                this.mHandler.obtainMessage(5).sendToTarget();
                break;
        }
    }
}
