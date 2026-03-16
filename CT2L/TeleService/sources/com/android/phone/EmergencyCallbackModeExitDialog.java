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
import android.os.SystemProperties;
import android.util.Log;
import com.android.internal.telephony.Phone;
import com.android.phone.EmergencyCallbackModeService;

public class EmergencyCallbackModeExitDialog extends Activity implements DialogInterface.OnDismissListener {
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
                    Log.w("EmergencyCallbackMode", "Failed to unbind from EmergencyCallbackModeService");
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
                EmergencyCallbackModeExitDialog.this.setResult(-1, new Intent().putExtra("exit_ecm_result", true));
                EmergencyCallbackModeExitDialog.this.finish();
            }
        }
    };
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            EmergencyCallbackModeExitDialog.this.mService = ((EmergencyCallbackModeService.LocalBinder) service).getService();
            synchronized (EmergencyCallbackModeExitDialog.this) {
                EmergencyCallbackModeExitDialog.this.notify();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            EmergencyCallbackModeExitDialog.this.mService = null;
        }
    };
    private Handler mTimerResetHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (!((Boolean) ((AsyncResult) msg.obj).result).booleanValue()) {
                        EmergencyCallbackModeExitDialog.this.setResult(-1, new Intent().putExtra("exit_ecm_result", false));
                        EmergencyCallbackModeExitDialog.this.finish();
                    }
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean isInEcm = Boolean.parseBoolean(SystemProperties.get("ril.cdma.inecmmode"));
        Log.i("EmergencyCallbackMode", "ECMModeExitDialog launched - isInEcm: " + isInEcm);
        if (!isInEcm) {
            finish();
            return;
        }
        this.mHandler = new Handler();
        Thread waitForConnectionCompleteThread = new Thread(null, this.mTask, "EcmExitDialogWaitThread");
        waitForConnectionCompleteThread.start();
        this.mPhone = PhoneGlobals.getPhone();
        this.mPhone.registerForEcmTimerReset(this.mTimerResetHandler, 1, (Object) null);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
        registerReceiver(this.mEcmExitReceiver, filter);
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
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        this.mDialogType = savedInstanceState.getInt("DIALOG_TYPE");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("DIALOG_TYPE", this.mDialogType);
    }

    private void showEmergencyCallbackModeExitDialog() {
        if (!isResumed()) {
            Log.w("EmergencyCallbackMode", "Tried to show dialog, but activity was already finished");
            return;
        }
        if (this.mInEmergencyCall) {
            this.mDialogType = 4;
            showDialog(4);
            return;
        }
        if (getIntent().getAction().equals("android.intent.action.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS")) {
            this.mDialogType = 1;
            showDialog(1);
        } else if (getIntent().getAction().equals("com.android.phone.action.ACTION_SHOW_ECM_EXIT_DIALOG")) {
            this.mDialogType = 2;
            showDialog(2);
        }
        this.mTimer = new CountDownTimer(this.mEcmTimeout, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                CharSequence text = EmergencyCallbackModeExitDialog.this.getDialogText(millisUntilFinished);
                EmergencyCallbackModeExitDialog.this.mAlertDialog.setMessage(text);
            }

            @Override
            public void onFinish() {
            }
        }.start();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case 1:
            case 2:
                CharSequence text = getDialogText(this.mEcmTimeout);
                this.mAlertDialog = new AlertDialog.Builder(this).setIcon(R.drawable.ic_emergency_callback_mode).setTitle(R.string.phone_in_ecm_notification_title).setMessage(text).setPositiveButton(R.string.alert_dialog_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        EmergencyCallbackModeExitDialog.this.mPhone.exitEmergencyCallbackMode();
                        EmergencyCallbackModeExitDialog.this.showDialog(3);
                        EmergencyCallbackModeExitDialog.this.mTimer.cancel();
                    }
                }).setNegativeButton(R.string.alert_dialog_no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        EmergencyCallbackModeExitDialog.this.setResult(-1, new Intent().putExtra("exit_ecm_result", false));
                        EmergencyCallbackModeExitDialog.this.finish();
                    }
                }).create();
                this.mAlertDialog.setOnDismissListener(this);
                break;
            case 3:
                this.mProgressDialog = new ProgressDialog(this);
                this.mProgressDialog.setMessage(getText(R.string.progress_dialog_exiting_ecm));
                this.mProgressDialog.setIndeterminate(true);
                this.mProgressDialog.setCancelable(false);
                break;
            case 4:
                this.mAlertDialog = new AlertDialog.Builder(this).setIcon(R.drawable.ic_emergency_callback_mode).setTitle(R.string.phone_in_ecm_notification_title).setMessage(R.string.alert_dialog_in_ecm_call).setNeutralButton(R.string.alert_dialog_dismiss, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        EmergencyCallbackModeExitDialog.this.setResult(-1, new Intent().putExtra("exit_ecm_result", false));
                        EmergencyCallbackModeExitDialog.this.finish();
                    }
                }).create();
                this.mAlertDialog.setOnDismissListener(this);
                break;
        }
        return this.mAlertDialog;
    }

    private CharSequence getDialogText(long millisUntilFinished) {
        int minutes = (int) (millisUntilFinished / 60000);
        String time = String.format("%d:%02d", Integer.valueOf(minutes), Long.valueOf((millisUntilFinished % 60000) / 1000));
        switch (this.mDialogType) {
            case 1:
                return String.format(getResources().getQuantityText(R.plurals.alert_dialog_not_avaialble_in_ecm, minutes).toString(), time);
            case 2:
                return String.format(getResources().getQuantityText(R.plurals.alert_dialog_exit_ecm, minutes).toString(), time);
            default:
                return null;
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        setResult(-1, new Intent().putExtra("exit_ecm_result", false));
        finish();
    }
}
