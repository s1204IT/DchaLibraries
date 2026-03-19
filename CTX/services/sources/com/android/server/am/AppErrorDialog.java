package com.android.server.am;

import android.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.BidiFormatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

final class AppErrorDialog extends BaseErrorDialog implements View.OnClickListener {
    static final int APP_INFO = 8;
    static final int CANCEL = 7;
    static final long DISMISS_TIMEOUT = 300000;
    static final int FORCE_QUIT = 1;
    static final int FORCE_QUIT_AND_REPORT = 2;
    static final int MUTE = 5;
    static final int RESTART = 3;
    static final int TIMEOUT = 6;
    private final Handler mHandler;
    private final boolean mIsRestartable;
    private final ProcessRecord mProc;
    private final BroadcastReceiver mReceiver;
    private final AppErrorResult mResult;
    private final ActivityManagerService mService;
    static int CANT_SHOW = -1;
    static int BACKGROUND_USER = -2;
    static int ALREADY_SHOWING = -3;

    public AppErrorDialog(Context context, ActivityManagerService activityManagerService, Data data) {
        CharSequence applicationLabel;
        super(context);
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                AppErrorDialog.this.setResult(message.what);
                AppErrorDialog.this.dismiss();
            }
        };
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                    AppErrorDialog.this.cancel();
                }
            }
        };
        Resources resources = context.getResources();
        this.mService = activityManagerService;
        this.mProc = data.proc;
        this.mResult = data.result;
        this.mIsRestartable = (data.task != null || data.isRestartableForService) && Settings.Global.getInt(context.getContentResolver(), "show_restart_in_crash_dialog", 0) != 0;
        BidiFormatter bidiFormatter = BidiFormatter.getInstance();
        if (this.mProc.pkgList.size() == 1 && (applicationLabel = context.getPackageManager().getApplicationLabel(this.mProc.info)) != null) {
            setTitle(resources.getString(data.repeating ? R.string.Midnight : R.string.EmergencyCallWarningTitle, bidiFormatter.unicodeWrap(applicationLabel.toString()), bidiFormatter.unicodeWrap(this.mProc.info.processName)));
        } else {
            setTitle(resources.getString(data.repeating ? R.string.PERSOSUBSTATE_RUIM_CORPORATE_ERROR : R.string.PERSOSUBSTATE_RUIM_CORPORATE_ENTRY, bidiFormatter.unicodeWrap(this.mProc.processName.toString())));
        }
        setCancelable(true);
        setCancelMessage(this.mHandler.obtainMessage(7));
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.setTitle("Application Error: " + this.mProc.info.processName);
        attributes.privateFlags = attributes.privateFlags | 272;
        getWindow().setAttributes(attributes);
        if (this.mProc.persistent) {
            getWindow().setType(2010);
        }
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(6), 300000L);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.custom);
        Context context = getContext();
        LayoutInflater.from(context).inflate(R.layout.alert_dialog_progress, (ViewGroup) frameLayout, true);
        boolean z = this.mProc.errorReportReceiver != null;
        TextView textView = (TextView) findViewById(R.id.KEYCODE_W);
        textView.setOnClickListener(this);
        textView.setVisibility(this.mIsRestartable ? 0 : 8);
        TextView textView2 = (TextView) findViewById(R.id.KEYCODE_VOLUME_UP);
        textView2.setOnClickListener(this);
        textView2.setVisibility(z ? 0 : 8);
        ((TextView) findViewById(R.id.KEYCODE_VOLUME_DOWN)).setOnClickListener(this);
        ((TextView) findViewById(R.id.KEYCODE_VOICE_ASSIST)).setOnClickListener(this);
        boolean z2 = (Build.IS_USER || Settings.Global.getInt(context.getContentResolver(), "development_settings_enabled", 0) == 0 || Settings.Global.getInt(context.getContentResolver(), "show_mute_in_crash_dialog", 0) == 0) ? false : true;
        TextView textView3 = (TextView) findViewById(R.id.KEYCODE_VOLUME_MUTE);
        textView3.setOnClickListener(this);
        textView3.setVisibility(z2 ? 0 : 8);
        findViewById(R.id.animation).setVisibility(0);
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
            setResult(1);
        }
        super.dismiss();
    }

    private void setResult(int i) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (this.mProc != null && this.mProc.crashDialog == this) {
                    this.mProc.crashDialog = null;
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        this.mResult.set(i);
        this.mHandler.removeMessages(6);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.KEYCODE_VOICE_ASSIST:
                this.mHandler.obtainMessage(8).sendToTarget();
                break;
            case R.id.KEYCODE_VOLUME_DOWN:
                this.mHandler.obtainMessage(1).sendToTarget();
                break;
            case R.id.KEYCODE_VOLUME_MUTE:
                this.mHandler.obtainMessage(5).sendToTarget();
                break;
            case R.id.KEYCODE_VOLUME_UP:
                this.mHandler.obtainMessage(2).sendToTarget();
                break;
            case R.id.KEYCODE_W:
                this.mHandler.obtainMessage(3).sendToTarget();
                break;
        }
    }

    static class Data {
        boolean isRestartableForService;
        ProcessRecord proc;
        boolean repeating;
        AppErrorResult result;
        TaskRecord task;

        Data() {
        }
    }
}
