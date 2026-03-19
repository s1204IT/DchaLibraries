package com.android.services.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.telecom.TelecomManager;
import com.android.internal.telephony.Phone;
import com.mediatek.phone.ext.ExtensionManager;

final class TtyManager {
    private static final int MSG_GET_TTY_MODE_RESPONSE = 2;
    private static final int MSG_SET_TTY_MODE_RESPONSE = 1;
    private final Phone mPhone;
    private int mTtyMode;
    private final TtyBroadcastReceiver mReceiver = new TtyBroadcastReceiver();
    private int mUiTtyMode = -1;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    Log.v(TtyManager.this, "got setTtyMode response", new Object[0]);
                    AsyncResult asyncResult = (AsyncResult) message.obj;
                    if (asyncResult.exception != null) {
                        Log.d(TtyManager.this, "setTTYMode exception: %s", asyncResult.exception);
                    }
                    TtyManager.this.mPhone.queryTTYMode(obtainMessage(2));
                    break;
                case 2:
                    Log.v(TtyManager.this, "got queryTTYMode response", new Object[0]);
                    AsyncResult asyncResult2 = (AsyncResult) message.obj;
                    if (asyncResult2.exception == null) {
                        int iPhoneModeToTelecomMode = TtyManager.phoneModeToTelecomMode(((int[]) asyncResult2.result)[0]);
                        if (iPhoneModeToTelecomMode != TtyManager.this.mTtyMode) {
                            Log.d(TtyManager.this, "setting TTY mode failed, attempted %d, got: %d", Integer.valueOf(TtyManager.this.mTtyMode), Integer.valueOf(iPhoneModeToTelecomMode));
                            ExtensionManager.getGttInfoExt().writeSettingsAndShowToast(iPhoneModeToTelecomMode);
                        } else {
                            Log.d(TtyManager.this, "setting TTY mode to %d succeeded", Integer.valueOf(iPhoneModeToTelecomMode));
                            ExtensionManager.getGttInfoExt().updateAudioTtyMode(iPhoneModeToTelecomMode);
                        }
                    } else {
                        Log.d(TtyManager.this, "queryTTYMode exception: %s", asyncResult2.exception);
                    }
                    break;
            }
        }
    };

    TtyManager(Context context, Phone phone) {
        int currentTtyMode;
        this.mPhone = phone;
        IntentFilter intentFilter = new IntentFilter("android.telecom.action.CURRENT_TTY_MODE_CHANGED");
        intentFilter.addAction("android.telecom.action.TTY_PREFERRED_MODE_CHANGED");
        context.registerReceiver(this.mReceiver, intentFilter);
        TelecomManager telecomManagerFrom = TelecomManager.from(context);
        if (telecomManagerFrom != null) {
            currentTtyMode = telecomManagerFrom.getCurrentTtyMode();
        } else {
            currentTtyMode = 0;
        }
        updateTtyMode(currentTtyMode);
        updateUiTtyMode(Settings.Secure.getInt(context.getContentResolver(), "preferred_tty_mode", 0));
    }

    private void updateTtyMode(int i) {
        Log.v(this, "updateTtyMode %d -> %d", Integer.valueOf(this.mTtyMode), Integer.valueOf(i));
        this.mTtyMode = i;
        this.mPhone.setTTYMode(telecomModeToPhoneMode(i), this.mHandler.obtainMessage(1));
    }

    private void updateUiTtyMode(int i) {
        Log.i(this, "updateUiTtyMode %d -> %d", Integer.valueOf(this.mUiTtyMode), Integer.valueOf(i));
        if (this.mUiTtyMode != i) {
            this.mUiTtyMode = i;
            this.mPhone.setUiTTYMode(telecomModeToPhoneMode(i), (Message) null);
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
                TtyManager.this.updateTtyMode(intent.getIntExtra("android.telecom.intent.extra.CURRENT_TTY_MODE", 0));
            } else if (action.equals("android.telecom.action.TTY_PREFERRED_MODE_CHANGED")) {
                TtyManager.this.updateUiTtyMode(intent.getIntExtra("android.telecom.intent.extra.TTY_PREFERRED", 0));
            }
        }
    }

    private static int telecomModeToPhoneMode(int i) {
        switch (i) {
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

    private static int phoneModeToTelecomMode(int i) {
        switch (i) {
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
