package com.android.settings.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.util.Log;
import android.widget.Switch;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.search.Index;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.WirelessUtils;
import com.mediatek.settings.PDebug;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.util.concurrent.atomic.AtomicBoolean;

public class WifiEnabler implements SwitchBar.OnSwitchChangeListener {
    private Context mContext;
    private boolean mStateMachineEvent;
    private SwitchBar mSwitchBar;
    private final WifiManager mWifiManager;
    private boolean mListeningToOnSwitchChange = false;
    private AtomicBoolean mConnected = new AtomicBoolean(false);
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.net.wifi.WIFI_STATE_CHANGED".equals(action)) {
                WifiEnabler.this.handleWifiStateChanged(intent.getIntExtra("wifi_state", 4));
                return;
            }
            if ("android.net.wifi.supplicant.STATE_CHANGE".equals(action)) {
                if (WifiEnabler.this.mConnected.get()) {
                    return;
                }
                WifiEnabler.this.handleStateChanged(android.net.wifi.WifiInfo.getDetailedStateOf((SupplicantState) intent.getParcelableExtra("newState")));
            } else {
                if (!"android.net.wifi.STATE_CHANGE".equals(action)) {
                    return;
                }
                NetworkInfo info = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                WifiEnabler.this.mConnected.set(info.isConnected());
                WifiEnabler.this.handleStateChanged(info.getDetailedState());
            }
        }
    };
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DefaultWfcSettingsExt.RESUME:
                    boolean isWiFiOn = msg.getData().getBoolean("is_wifi_on");
                    Index.getInstance(WifiEnabler.this.mContext).updateFromClassNameResource(WifiSettings.class.getName(), true, isWiFiOn);
                    break;
            }
        }
    };
    private final IntentFilter mIntentFilter = new IntentFilter("android.net.wifi.WIFI_STATE_CHANGED");

    public WifiEnabler(Context context, SwitchBar switchBar) {
        this.mContext = context;
        this.mSwitchBar = switchBar;
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mIntentFilter.addAction("android.net.wifi.supplicant.STATE_CHANGE");
        this.mIntentFilter.addAction("android.net.wifi.STATE_CHANGE");
        setupSwitchBar();
    }

    public void setupSwitchBar() {
        int state = this.mWifiManager.getWifiState();
        handleWifiStateChanged(state);
        if (!this.mListeningToOnSwitchChange) {
            this.mSwitchBar.addOnSwitchChangeListener(this);
            this.mListeningToOnSwitchChange = true;
        }
        this.mSwitchBar.show();
    }

    public void teardownSwitchBar() {
        if (this.mListeningToOnSwitchChange) {
            this.mSwitchBar.removeOnSwitchChangeListener(this);
            this.mListeningToOnSwitchChange = false;
        }
        this.mSwitchBar.hide();
    }

    public void resume(Context context) {
        PDebug.Start("WifiEnabler.resume");
        this.mContext = context;
        this.mContext.registerReceiver(this.mReceiver, this.mIntentFilter);
        if (!this.mListeningToOnSwitchChange) {
            this.mSwitchBar.addOnSwitchChangeListener(this);
            this.mListeningToOnSwitchChange = true;
        }
        PDebug.End("WifiEnabler.resume");
    }

    public void pause() {
        this.mContext.unregisterReceiver(this.mReceiver);
        if (!this.mListeningToOnSwitchChange) {
            return;
        }
        this.mSwitchBar.removeOnSwitchChangeListener(this);
        this.mListeningToOnSwitchChange = false;
    }

    public void handleWifiStateChanged(int state) {
        Log.d("WifiEnabler", "handleWifiStateChanged, state = " + state);
        this.mSwitchBar.setDisabledByAdmin(null);
        switch (state) {
            case DefaultWfcSettingsExt.RESUME:
                this.mSwitchBar.setEnabled(false);
                break;
            case DefaultWfcSettingsExt.PAUSE:
                setSwitchBarChecked(false);
                this.mSwitchBar.setEnabled(true);
                updateSearchIndex(false);
                break;
            case DefaultWfcSettingsExt.CREATE:
                this.mSwitchBar.setEnabled(false);
                break;
            case DefaultWfcSettingsExt.DESTROY:
                setSwitchBarChecked(true);
                this.mSwitchBar.setEnabled(true);
                updateSearchIndex(true);
                break;
            default:
                setSwitchBarChecked(false);
                this.mSwitchBar.setEnabled(true);
                updateSearchIndex(false);
                break;
        }
        if (!mayDisableTethering(this.mSwitchBar.isChecked() ? false : true)) {
            return;
        }
        if (RestrictedLockUtils.hasBaseUserRestriction(this.mContext, "no_config_tethering", UserHandle.myUserId())) {
            this.mSwitchBar.setEnabled(false);
        } else {
            RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(this.mContext, "no_config_tethering", UserHandle.myUserId());
            this.mSwitchBar.setDisabledByAdmin(admin);
        }
    }

    private void updateSearchIndex(boolean isWiFiOn) {
        this.mHandler.removeMessages(0);
        Message msg = new Message();
        msg.what = 0;
        msg.getData().putBoolean("is_wifi_on", isWiFiOn);
        this.mHandler.sendMessage(msg);
    }

    private void setSwitchBarChecked(boolean checked) {
        Log.d("WifiEnabler", "setSwitchChecked, checked = " + checked);
        this.mStateMachineEvent = true;
        this.mSwitchBar.setChecked(checked);
        this.mStateMachineEvent = false;
    }

    public void handleStateChanged(NetworkInfo.DetailedState state) {
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        Log.d("WifiEnabler", "onCheckedChanged, isChecked = " + isChecked);
        if (this.mStateMachineEvent) {
            return;
        }
        if (isChecked && !WirelessUtils.isRadioAllowed(this.mContext, "wifi")) {
            Toast.makeText(this.mContext, R.string.wifi_in_airplane_mode, 0).show();
            this.mSwitchBar.setChecked(false);
            return;
        }
        if (mayDisableTethering(isChecked)) {
            this.mWifiManager.setWifiApEnabled(null, false);
        }
        Log.d("WifiEnabler", "onCheckedChanged, setWifiEnabled = " + isChecked);
        MetricsLogger.action(this.mContext, isChecked ? 139 : 138);
        if (this.mWifiManager.setWifiEnabled(isChecked)) {
            return;
        }
        this.mSwitchBar.setEnabled(true);
        Toast.makeText(this.mContext, R.string.wifi_error, 0).show();
    }

    private boolean mayDisableTethering(boolean isChecked) {
        int wifiApState = this.mWifiManager.getWifiApState();
        if (isChecked) {
            return wifiApState == 12 || wifiApState == 13;
        }
        return false;
    }
}
