package com.android.settings.wifi.tether;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import com.android.settings.datausage.DataSaverBackend;
import com.android.settings.widget.SwitchWidgetController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.wifi.AccessPoint;

/* loaded from: classes.dex */
public class WifiTetherSwitchBarController implements DataSaverBackend.Listener, SwitchWidgetController.OnSwitchChangeListener, LifecycleObserver, OnStart, OnStop {
    private static final IntentFilter WIFI_INTENT_FILTER = new IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED");
    private final ConnectivityManager mConnectivityManager;
    private final Context mContext;
    final DataSaverBackend mDataSaverBackend;
    final ConnectivityManager.OnStartTetheringCallback mOnStartTetheringCallback = new ConnectivityManager.OnStartTetheringCallback() { // from class: com.android.settings.wifi.tether.WifiTetherSwitchBarController.1
        public void onTetheringFailed() {
            super.onTetheringFailed();
            WifiTetherSwitchBarController.this.mSwitchBar.setChecked(false);
            WifiTetherSwitchBarController.this.updateWifiSwitch();
        }
    };
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() { // from class: com.android.settings.wifi.tether.WifiTetherSwitchBarController.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(action)) {
                WifiTetherSwitchBarController.this.handleWifiApStateChanged(intent.getIntExtra("wifi_state", 14));
            } else if ("android.intent.action.AIRPLANE_MODE".equals(action)) {
                WifiTetherSwitchBarController.this.updateWifiSwitch();
            }
        }
    };
    private final SwitchWidgetController mSwitchBar;
    private final WifiManager mWifiManager;

    static {
        WIFI_INTENT_FILTER.addAction("android.intent.action.AIRPLANE_MODE");
    }

    WifiTetherSwitchBarController(Context context, SwitchWidgetController switchWidgetController) {
        this.mContext = context;
        this.mSwitchBar = switchWidgetController;
        this.mDataSaverBackend = new DataSaverBackend(context);
        this.mConnectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mSwitchBar.setChecked(this.mWifiManager.getWifiApState() == 13);
        this.mSwitchBar.setListener(this);
        updateWifiSwitch();
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnStart
    public void onStart() {
        this.mDataSaverBackend.addListener(this);
        this.mSwitchBar.startListening();
        this.mContext.registerReceiver(this.mReceiver, WIFI_INTENT_FILTER);
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnStop
    public void onStop() {
        this.mDataSaverBackend.remListener(this);
        this.mSwitchBar.stopListening();
        this.mContext.unregisterReceiver(this.mReceiver);
    }

    @Override // com.android.settings.widget.SwitchWidgetController.OnSwitchChangeListener
    public boolean onSwitchToggled(boolean z) {
        if (!z) {
            stopTether();
            return true;
        }
        if (!this.mWifiManager.isWifiApEnabled()) {
            startTether();
            return true;
        }
        return true;
    }

    void stopTether() {
        this.mSwitchBar.setEnabled(false);
        this.mConnectivityManager.stopTethering(0);
    }

    void startTether() {
        this.mSwitchBar.setEnabled(false);
        this.mConnectivityManager.startTethering(0, true, this.mOnStartTetheringCallback, new Handler(Looper.getMainLooper()));
    }

    private void handleWifiApStateChanged(int i) {
        switch (i) {
            case AccessPoint.Speed.MODERATE /* 10 */:
                if (this.mSwitchBar.isChecked()) {
                    this.mSwitchBar.setChecked(false);
                }
                this.mSwitchBar.setEnabled(false);
                break;
            case 11:
                this.mSwitchBar.setChecked(false);
                updateWifiSwitch();
                break;
            case 12:
                this.mSwitchBar.setEnabled(false);
                break;
            case 13:
                if (!this.mSwitchBar.isChecked()) {
                    this.mSwitchBar.setChecked(true);
                }
                updateWifiSwitch();
                break;
            default:
                this.mSwitchBar.setChecked(false);
                updateWifiSwitch();
                break;
        }
    }

    private void updateWifiSwitch() {
        if (!(Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) != 0)) {
            this.mSwitchBar.setEnabled(true ^ this.mDataSaverBackend.isDataSaverEnabled());
        } else {
            this.mSwitchBar.setEnabled(false);
        }
    }

    @Override // com.android.settings.datausage.DataSaverBackend.Listener
    public void onDataSaverChanged(boolean z) {
        updateWifiSwitch();
    }

    @Override // com.android.settings.datausage.DataSaverBackend.Listener
    public void onWhitelistStatusChanged(int i, boolean z) {
    }

    @Override // com.android.settings.datausage.DataSaverBackend.Listener
    public void onBlacklistStatusChanged(int i, boolean z) {
    }
}
