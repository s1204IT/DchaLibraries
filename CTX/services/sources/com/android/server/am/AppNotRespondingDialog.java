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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;

public final class AppNotRespondingDialog extends BaseErrorDialog implements View.OnClickListener {
    public static final int ALREADY_SHOWING = -2;
    public static final int CANT_SHOW = -1;
    static final int FORCE_CLOSE = 1;
    private static final String TAG = "AppNotRespondingDialog";
    static final int WAIT = 2;
    static final int WAIT_AND_REPORT = 3;
    private final Handler mHandler;
    private final ProcessRecord mProc;
    private final ActivityManagerService mService;

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        return super.dispatchKeyEvent(keyEvent);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    public AppNotRespondingDialog(ActivityManagerService activityManagerService, Context context, Data data) {
        CharSequence charSequenceLoadLabel;
        int i;
        String string;
        super(context);
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                Intent intentCreateAppErrorIntentLocked;
                MetricsLogger.action(AppNotRespondingDialog.this.getContext(), 317, message.what);
                switch (message.what) {
                    case 1:
                        AppNotRespondingDialog.this.mService.killAppAtUsersRequest(AppNotRespondingDialog.this.mProc, AppNotRespondingDialog.this);
                        intentCreateAppErrorIntentLocked = null;
                        if (intentCreateAppErrorIntentLocked != null) {
                            try {
                                AppNotRespondingDialog.this.getContext().startActivity(intentCreateAppErrorIntentLocked);
                            } catch (ActivityNotFoundException e) {
                                Slog.w(AppNotRespondingDialog.TAG, "bug report receiver dissappeared", e);
                            }
                            break;
                        }
                        AppNotRespondingDialog.this.dismiss();
                        return;
                    case 2:
                    case 3:
                        synchronized (AppNotRespondingDialog.this.mService) {
                            try {
                                ActivityManagerService.boostPriorityForLockedSection();
                                ProcessRecord processRecord = AppNotRespondingDialog.this.mProc;
                                if (message.what == 3) {
                                    intentCreateAppErrorIntentLocked = AppNotRespondingDialog.this.mService.mAppErrors.createAppErrorIntentLocked(processRecord, System.currentTimeMillis(), null);
                                } else {
                                    intentCreateAppErrorIntentLocked = null;
                                }
                                processRecord.notResponding = false;
                                processRecord.notRespondingReport = null;
                                if (processRecord.anrDialog == AppNotRespondingDialog.this) {
                                    processRecord.anrDialog = null;
                                }
                                AppNotRespondingDialog.this.mService.mServices.scheduleServiceTimeoutLocked(processRecord);
                            } catch (Throwable th) {
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                throw th;
                            }
                            break;
                        }
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        if (intentCreateAppErrorIntentLocked != null) {
                        }
                        AppNotRespondingDialog.this.dismiss();
                        return;
                    default:
                        intentCreateAppErrorIntentLocked = null;
                        if (intentCreateAppErrorIntentLocked != null) {
                        }
                        AppNotRespondingDialog.this.dismiss();
                        return;
                }
            }
        };
        this.mService = activityManagerService;
        this.mProc = data.proc;
        Resources resources = context.getResources();
        setCancelable(false);
        CharSequence applicationLabel = null;
        if (data.activity != null) {
            charSequenceLoadLabel = data.activity.info.loadLabel(context.getPackageManager());
        } else {
            charSequenceLoadLabel = null;
        }
        if (this.mProc.pkgList.size() == 1 && (applicationLabel = context.getPackageManager().getApplicationLabel(this.mProc.info)) != null) {
            if (charSequenceLoadLabel != null) {
                i = R.string.PERSOSUBSTATE_RUIM_NETWORK1_PUK_SUCCESS;
            } else {
                applicationLabel = this.mProc.processName;
                i = 17039482;
                charSequenceLoadLabel = applicationLabel;
            }
        } else if (charSequenceLoadLabel != null) {
            applicationLabel = this.mProc.processName;
            i = R.string.PERSOSUBSTATE_RUIM_NETWORK1_SUCCESS;
        } else {
            charSequenceLoadLabel = this.mProc.processName;
            i = R.string.PERSOSUBSTATE_RUIM_NETWORK2_ERROR;
        }
        BidiFormatter bidiFormatter = BidiFormatter.getInstance();
        if (applicationLabel != null) {
            string = resources.getString(i, bidiFormatter.unicodeWrap(charSequenceLoadLabel.toString()), bidiFormatter.unicodeWrap(applicationLabel.toString()));
        } else {
            string = resources.getString(i, bidiFormatter.unicodeWrap(charSequenceLoadLabel.toString()));
        }
        setTitle(string);
        if (data.aboveSystem) {
            getWindow().setType(2010);
        }
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.setTitle("Application Not Responding: " + this.mProc.info.processName);
        attributes.privateFlags = 272;
        getWindow().setAttributes(attributes);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        LayoutInflater.from(getContext()).inflate(R.layout.alert_dialog_material, (ViewGroup) findViewById(R.id.custom), true);
        TextView textView = (TextView) findViewById(R.id.KEYCODE_VOLUME_UP);
        textView.setOnClickListener(this);
        textView.setVisibility(this.mProc.errorReportReceiver != null ? 0 : 8);
        ((TextView) findViewById(R.id.KEYCODE_VOLUME_DOWN)).setOnClickListener(this);
        ((TextView) findViewById(R.id.KEYCODE_WINDOW)).setOnClickListener(this);
        findViewById(R.id.animation).setVisibility(0);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == 16908694) {
            this.mHandler.obtainMessage(1).sendToTarget();
        } else if (id == 16908696) {
            this.mHandler.obtainMessage(3).sendToTarget();
        } else if (id == 16908698) {
            this.mHandler.obtainMessage(2).sendToTarget();
        }
    }

    public static class Data {
        final boolean aboveSystem;
        final ActivityRecord activity;
        final ProcessRecord proc;

        public Data(ProcessRecord processRecord, ActivityRecord activityRecord, boolean z) {
            this.proc = processRecord;
            this.activity = activityRecord;
            this.aboveSystem = z;
        }
    }
}
