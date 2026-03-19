package com.android.phone;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.android.internal.telephony.Phone;
import com.android.phone.EmergencyCallbackModeService;
import com.mediatek.phone.ext.ExtensionManager;

public class EmergencyCallbackModeExitDialog extends Activity implements DialogInterface.OnCancelListener {
    static final String ACTION_SHOW_ECM_EXIT_DIALOG = "com.android.phone.action.ACTION_SHOW_ECM_EXIT_DIALOG";
    private static final int ECM_TIMER_RESET = 1;
    public static final int EXIT_ECM_BLOCK_OTHERS = 1;
    public static final int EXIT_ECM_DIALOG = 2;
    public static final int EXIT_ECM_IN_EMERGENCY_CALL_DIALOG = 4;
    public static final int EXIT_ECM_PROGRESS_DIALOG = 3;
    public static final String EXTRA_EXIT_ECM_RESULT = "exit_ecm_result";
    private static final String TAG = "EmergencyCallbackMode";
    AlertDialog mAlertDialog = null;
    ProgressDialog mProgressDialog = null;
    CountDownTimer mTimer = null;
    EmergencyCallbackModeService mService = null;
    Handler mHandler = null;
    int mDialogType = 0;
    long mEcmTimeout = 0;
    private boolean mInEmergencyCall = false;
    private Phone mPhone = null;
    private Runnable mTask = new Runnable() {
        @Override
        public void run() {
            Looper.prepare();
            EmergencyCallbackModeExitDialog.this.bindService(new Intent(EmergencyCallbackModeExitDialog.this, (Class<?>) EmergencyCallbackModeService.class), EmergencyCallbackModeExitDialog.this.mConnection, 1);
            synchronized (EmergencyCallbackModeExitDialog.this) {
                try {
                } catch (InterruptedException e) {
                    Log.d("ECM", "EmergencyCallbackModeExitDialog InterruptedException: " + e.getMessage());
                    e.printStackTrace();
                }
                if (EmergencyCallbackModeExitDialog.this.mService == null) {
                    EmergencyCallbackModeExitDialog.this.wait();
                }
            }
            if (EmergencyCallbackModeExitDialog.this.mService != null) {
                EmergencyCallbackModeExitDialog.this.mEcmTimeout = EmergencyCallbackModeExitDialog.this.mService.getEmergencyCallbackModeTimeout();
                EmergencyCallbackModeExitDialog.this.mInEmergencyCall = EmergencyCallbackModeExitDialog.this.mService.getEmergencyCallbackModeCallState();
                try {
                    EmergencyCallbackModeExitDialog.this.unbindService(EmergencyCallbackModeExitDialog.this.mConnection);
                } catch (IllegalArgumentException e2) {
                    Log.w(EmergencyCallbackModeExitDialog.TAG, "Failed to unbind from EmergencyCallbackModeService");
                }
            }
            EmergencyCallbackModeExitDialog.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    EmergencyCallbackModeExitDialog.this.showEmergencyCallbackModeExitDialog();
                }
            });
        }
    };
    private BroadcastReceiver mEcmExitReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED") && !intent.getBooleanExtra("phoneinECMState", false)) {
                if (EmergencyCallbackModeExitDialog.this.mAlertDialog != null) {
                    EmergencyCallbackModeExitDialog.this.mAlertDialog.dismiss();
                }
                if (EmergencyCallbackModeExitDialog.this.mProgressDialog != null) {
                    EmergencyCallbackModeExitDialog.this.mProgressDialog.dismiss();
                }
                EmergencyCallbackModeExitDialog.this.setResult(-1, new Intent().putExtra(EmergencyCallbackModeExitDialog.EXTRA_EXIT_ECM_RESULT, true));
                EmergencyCallbackModeExitDialog.this.finish();
            }
        }
    };
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            EmergencyCallbackModeExitDialog.this.mService = ((EmergencyCallbackModeService.LocalBinder) iBinder).getService();
            synchronized (EmergencyCallbackModeExitDialog.this) {
                EmergencyCallbackModeExitDialog.this.notify();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            EmergencyCallbackModeExitDialog.this.mService = null;
        }
    };
    private Handler mTimerResetHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            if (message.what == 1 && !((Boolean) ((AsyncResult) message.obj).result).booleanValue()) {
                EmergencyCallbackModeExitDialog.this.setResult(-1, new Intent().putExtra(EmergencyCallbackModeExitDialog.EXTRA_EXIT_ECM_RESULT, false));
                EmergencyCallbackModeExitDialog.this.finish();
            }
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mPhone = PhoneGlobals.getInstance().getPhoneInEcm();
        if (this.mPhone == null || !this.mPhone.isInEcm()) {
            Log.i(TAG, "ECMModeExitDialog launched - isInEcm: false phone:" + this.mPhone);
            setResult(-1, new Intent().putExtra(EXTRA_EXIT_ECM_RESULT, false));
            finish();
            return;
        }
        Log.i(TAG, "ECMModeExitDialog launched - isInEcm: true phone:" + this.mPhone);
        this.mHandler = new Handler();
        new Thread(null, this.mTask, "EcmExitDialogWaitThread").start();
        this.mPhone.registerForEcmTimerReset(this.mTimerResetHandler, 1, (Object) null);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
        registerReceiver(this.mEcmExitReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(this.mEcmExitReceiver);
        } catch (IllegalArgumentException e) {
        }
        if (this.mPhone != null) {
            this.mPhone.unregisterForEcmTimerReset(this.mHandler);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        this.mDialogType = bundle.getInt("DIALOG_TYPE");
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt("DIALOG_TYPE", this.mDialogType);
    }

    private void showEmergencyCallbackModeExitDialog() {
        if (!isResumed()) {
            Log.w(TAG, "Tried to show dialog, but activity was already finished");
            setResult(-1, new Intent().putExtra(EXTRA_EXIT_ECM_RESULT, false));
            finish();
        } else {
            if (this.mInEmergencyCall) {
                this.mDialogType = 4;
                showDialog(4);
                return;
            }
            if (getIntent().getAction().equals("com.android.internal.intent.action.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS")) {
                this.mDialogType = 1;
                showDialog(1);
            } else if (getIntent().getAction().equals(ACTION_SHOW_ECM_EXIT_DIALOG)) {
                this.mDialogType = 2;
                showDialog(2);
            }
            this.mTimer = new CountDownTimer(this.mEcmTimeout, 1000L) {
                @Override
                public void onTick(long j) {
                    EmergencyCallbackModeExitDialog.this.mAlertDialog.setMessage(EmergencyCallbackModeExitDialog.this.getDialogText(j));
                }

                @Override
                public void onFinish() {
                }
            }.start();
        }
    }

    @Override
    protected Dialog onCreateDialog(int i) {
        switch (i) {
            case 1:
            case 2:
                this.mAlertDialog = new AlertDialog.Builder(this, android.R.style.Theme.DeviceDefault.Dialog.Alert).setIcon(R.drawable.ic_emergency_callback_mode).setTitle(R.string.phone_in_ecm_notification_title).setMessage(getDialogText(this.mEcmTimeout)).setPositiveButton(R.string.alert_dialog_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i2) {
                        EmergencyCallbackModeExitDialog.this.mPhone.exitEmergencyCallbackMode();
                        EmergencyCallbackModeExitDialog.this.showDialog(3);
                        EmergencyCallbackModeExitDialog.this.mTimer.cancel();
                    }
                }).setNegativeButton(R.string.alert_dialog_no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i2) {
                        EmergencyCallbackModeExitDialog.this.setResult(-1, new Intent().putExtra(EmergencyCallbackModeExitDialog.EXTRA_EXIT_ECM_RESULT, false));
                        EmergencyCallbackModeExitDialog.this.finish();
                    }
                }).create();
                this.mAlertDialog.setOnCancelListener(this);
                return this.mAlertDialog;
            case 3:
                this.mProgressDialog = new ProgressDialog(this);
                this.mProgressDialog.setMessage(getText(R.string.progress_dialog_exiting_ecm));
                this.mProgressDialog.setIndeterminate(true);
                this.mProgressDialog.setCancelable(false);
                return this.mProgressDialog;
            case 4:
                this.mAlertDialog = new AlertDialog.Builder(this, android.R.style.Theme.DeviceDefault.Dialog.Alert).setIcon(R.drawable.ic_emergency_callback_mode).setTitle(R.string.phone_in_ecm_notification_title).setMessage(R.string.alert_dialog_in_ecm_call).setNeutralButton(R.string.alert_dialog_dismiss, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i2) {
                        EmergencyCallbackModeExitDialog.this.setResult(-1, new Intent().putExtra(EmergencyCallbackModeExitDialog.EXTRA_EXIT_ECM_RESULT, false));
                        EmergencyCallbackModeExitDialog.this.finish();
                    }
                }).create();
                this.mAlertDialog.setOnCancelListener(this);
                return this.mAlertDialog;
            default:
                return null;
        }
    }

    private CharSequence getDialogText(long j) {
        int i = (int) (j / 60000);
        String str = String.format("%d:%02d", Integer.valueOf(i), Long.valueOf((j % 60000) / 1000));
        String dialogText = ExtensionManager.getEmergencyDialerExt().getDialogText(this.mPhone, this.mDialogType, j);
        if (dialogText != null) {
            return dialogText;
        }
        switch (this.mDialogType) {
            case 1:
                return String.format(getResources().getQuantityText(R.plurals.alert_dialog_not_avaialble_in_ecm, i).toString(), str);
            case 2:
                return String.format(getResources().getQuantityText(R.plurals.alert_dialog_exit_ecm, i).toString(), str);
            default:
                return null;
        }
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        setResult(-1, new Intent().putExtra(EXTRA_EXIT_ECM_RESULT, false));
        finish();
    }
}
