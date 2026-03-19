package com.android.server.telecom;

import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telecom.Log;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class SystemStateProvider {
    private final Context mContext;
    private boolean mIsCarMode;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.startSession("SSP.oR");
            try {
                String action = intent.getAction();
                if (UiModeManager.ACTION_ENTER_CAR_MODE.equals(action)) {
                    SystemStateProvider.this.onEnterCarMode();
                } else if (UiModeManager.ACTION_EXIT_CAR_MODE.equals(action)) {
                    SystemStateProvider.this.onExitCarMode();
                } else {
                    Log.w(this, "Unexpected intent received: %s", new Object[]{intent.getAction()});
                }
            } finally {
                Log.endSession();
            }
        }
    };
    private Set<SystemStateListener> mListeners = new CopyOnWriteArraySet();

    public interface SystemStateListener {
        void onCarModeChanged(boolean z);
    }

    public SystemStateProvider(Context context) {
        this.mContext = context;
        IntentFilter intentFilter = new IntentFilter(UiModeManager.ACTION_ENTER_CAR_MODE);
        intentFilter.addAction(UiModeManager.ACTION_EXIT_CAR_MODE);
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter);
        Log.i(this, "Registering car mode receiver: %s", new Object[]{intentFilter});
        this.mIsCarMode = getSystemCarMode();
    }

    public void addListener(SystemStateListener systemStateListener) {
        if (systemStateListener != null) {
            this.mListeners.add(systemStateListener);
        }
    }

    public boolean isCarMode() {
        return this.mIsCarMode;
    }

    private void onEnterCarMode() {
        if (!this.mIsCarMode) {
            Log.i(this, "Entering carmode", new Object[0]);
            this.mIsCarMode = true;
            notifyCarMode();
        }
    }

    private void onExitCarMode() {
        if (this.mIsCarMode) {
            Log.i(this, "Exiting carmode", new Object[0]);
            this.mIsCarMode = false;
            notifyCarMode();
        }
    }

    private void notifyCarMode() {
        Iterator<SystemStateListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onCarModeChanged(this.mIsCarMode);
        }
    }

    private boolean getSystemCarMode() {
        UiModeManager uiModeManager = (UiModeManager) this.mContext.getSystemService("uimode");
        return uiModeManager != null && uiModeManager.getCurrentModeType() == 3;
    }
}
