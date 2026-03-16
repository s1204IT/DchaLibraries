package com.android.services.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telecom.TelecomManager;
import com.android.internal.telephony.Phone;

final class TtyManager {
    private final Phone mPhone;
    private int mTtyMode;
    private final TtyBroadcastReceiver mReceiver = new TtyBroadcastReceiver();
    private int mUiTtyMode = -1;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Log.v(TtyManager.this, "got setTtyMode response", new Object[0]);
                    AsyncResult ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        Log.d(TtyManager.this, "setTTYMode exception: %s", ar.exception);
                    }
                    TtyManager.this.mPhone.queryTTYMode(obtainMessage(2));
                    break;
                case 2:
                    Log.v(TtyManager.this, "got queryTTYMode response", new Object[0]);
                    AsyncResult ar2 = (AsyncResult) msg.obj;
                    if (ar2.exception == null) {
                        int ttyMode = TtyManager.phoneModeToTelecomMode(((int[]) ar2.result)[0]);
                        if (ttyMode != TtyManager.this.mTtyMode) {
                            Log.d(TtyManager.this, "setting TTY mode failed, attempted %d, got: %d", Integer.valueOf(TtyManager.this.mTtyMode), Integer.valueOf(ttyMode));
                        } else {
                            Log.d(TtyManager.this, "setting TTY mode to %d succeeded", Integer.valueOf(ttyMode));
                        }
                    } else {
                        Log.d(TtyManager.this, "queryTTYMode exception: %s", ar2.exception);
                    }
                    break;
            }
        }
    };

    TtyManager(Context context, Phone phone) {
        this.mPhone = phone;
        IntentFilter intentFilter = new IntentFilter("android.telecom.action.CURRENT_TTY_MODE_CHANGED");
        intentFilter.addAction("android.telecom.action.TTY_PREFERRED_MODE_CHANGED");
        context.registerReceiver(this.mReceiver, intentFilter);
        TelecomManager telecomManager = TelecomManager.from(context);
        int ttyMode = telecomManager != null ? telecomManager.getCurrentTtyMode() : 0;
        updateTtyMode(ttyMode);
        updateUiTtyMode(ttyMode);
    }

    private void updateTtyMode(int ttyMode) {
        Log.v(this, "updateTtyMode %d -> %d", Integer.valueOf(this.mTtyMode), Integer.valueOf(ttyMode));
        this.mTtyMode = ttyMode;
        this.mPhone.setTTYMode(telecomModeToPhoneMode(ttyMode), this.mHandler.obtainMessage(1));
    }

    private void updateUiTtyMode(int ttyMode) {
        Log.i(this, "updateUiTtyMode %d -> %d", Integer.valueOf(this.mUiTtyMode), Integer.valueOf(ttyMode));
        if (this.mUiTtyMode != ttyMode) {
            this.mUiTtyMode = ttyMode;
            this.mPhone.setUiTTYMode(telecomModeToPhoneMode(ttyMode), (Message) null);
        } else {
            Log.i(this, "ui tty mode didnt change", new Object[0]);
        }
    }

    private final class TtyBroadcastReceiver extends BroadcastReceiver {
        private TtyBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.v(TtyManager.this, "onReceive, action: %s", action);
            if (action.equals("android.telecom.action.CURRENT_TTY_MODE_CHANGED")) {
                int ttyMode = intent.getIntExtra("android.telecom.intent.extra.CURRENT_TTY_MODE", 0);
                TtyManager.this.updateTtyMode(ttyMode);
            } else if (action.equals("android.telecom.action.TTY_PREFERRED_MODE_CHANGED")) {
                int newPreferredTtyMode = intent.getIntExtra("android.telecom.intent.extra.TTY_PREFERRED", 0);
                TtyManager.this.updateUiTtyMode(newPreferredTtyMode);
            }
        }
    }

    private static int telecomModeToPhoneMode(int telecomMode) {
        switch (telecomMode) {
            case 1:
            case 2:
            case 3:
                return 1;
            default:
                return 0;
        }
    }

    private static int phoneModeToTelecomMode(int phoneMode) {
        switch (phoneMode) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            default:
                return 0;
        }
    }
}
