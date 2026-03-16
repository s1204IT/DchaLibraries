package com.android.server.telecom;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.UserHandle;
import android.provider.Settings;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.telecom.WiredHeadsetManager;

final class TtyManager implements WiredHeadsetManager.Listener {
    private final Context mContext;
    private int mPreferredTtyMode;
    private final WiredHeadsetManager mWiredHeadsetManager;
    private final TtyBroadcastReceiver mReceiver = new TtyBroadcastReceiver();
    private int mCurrentTtyMode = 0;

    TtyManager(Context context, WiredHeadsetManager wiredHeadsetManager) {
        this.mPreferredTtyMode = 0;
        this.mContext = context;
        this.mWiredHeadsetManager = wiredHeadsetManager;
        this.mWiredHeadsetManager.addListener(this);
        this.mPreferredTtyMode = Settings.Secure.getInt(this.mContext.getContentResolver(), "preferred_tty_mode", 0);
        this.mContext.registerReceiver(this.mReceiver, new IntentFilter("android.telecom.action.TTY_PREFERRED_MODE_CHANGED"));
        updateCurrentTtyMode();
    }

    boolean isTtySupported() {
        boolean z = this.mContext.getResources().getBoolean(R.bool.tty_enabled);
        Log.v(this, "isTtySupported: %b", Boolean.valueOf(z));
        return z;
    }

    int getCurrentTtyMode() {
        return this.mCurrentTtyMode;
    }

    @Override
    public void onWiredHeadsetPluggedInChanged(boolean z, boolean z2) {
        Log.v(this, "onWiredHeadsetPluggedInChanged", new Object[0]);
        updateCurrentTtyMode();
    }

    private void updateCurrentTtyMode() {
        int i = (isTtySupported() && this.mWiredHeadsetManager.isPluggedIn()) ? this.mPreferredTtyMode : 0;
        Log.v(this, "updateCurrentTtyMode, %d -> %d", Integer.valueOf(this.mCurrentTtyMode), Integer.valueOf(i));
        if (this.mCurrentTtyMode != i) {
            this.mCurrentTtyMode = i;
            Intent intent = new Intent("android.telecom.action.CURRENT_TTY_MODE_CHANGED");
            intent.putExtra("android.telecom.intent.extra.CURRENT_TTY_MODE", this.mCurrentTtyMode);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            updateAudioTtyMode();
        }
    }

    private void updateAudioTtyMode() {
        String str;
        switch (this.mCurrentTtyMode) {
            case 1:
                str = "tty_full";
                break;
            case 2:
                str = "tty_hco";
                break;
            case 3:
                str = "tty_vco";
                break;
            default:
                str = "tty_off";
                break;
        }
        Log.v(this, "updateAudioTtyMode, %s", str);
        ((AudioManager) this.mContext.getSystemService("audio")).setParameters("tty_mode=" + str);
    }

    private final class TtyBroadcastReceiver extends BroadcastReceiver {
        private TtyBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            int intExtra;
            String action = intent.getAction();
            Log.v(TtyManager.this, "onReceive, action: %s", action);
            if (action.equals("android.telecom.action.TTY_PREFERRED_MODE_CHANGED") && TtyManager.this.mPreferredTtyMode != (intExtra = intent.getIntExtra("android.telecom.intent.extra.TTY_PREFERRED", 0))) {
                TtyManager.this.mPreferredTtyMode = intExtra;
                TtyManager.this.updateCurrentTtyMode();
            }
        }
    }

    public void dump(IndentingPrintWriter indentingPrintWriter) {
        indentingPrintWriter.println("mCurrentTtyMode: " + this.mCurrentTtyMode);
    }
}
