package com.android.settings.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.os.UserHandle;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.widget.SwitchWidgetController;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.WirelessUtils;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IWifiSettingsExt;
import java.util.concurrent.atomic.AtomicBoolean;
/* loaded from: classes.dex */
public class WifiEnabler implements SwitchWidgetController.OnSwitchChangeListener {
    private AtomicBoolean mConnected;
    private final ConnectivityManager mConnectivityManager;
    private Context mContext;
    private final IntentFilter mIntentFilter;
    private boolean mListeningToOnSwitchChange;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final BroadcastReceiver mReceiver;
    private boolean mStateMachineEvent;
    private final SwitchWidgetController mSwitchWidget;
    private final WifiManager mWifiManager;
    private IWifiSettingsExt mWifiSettingsExt;

    public WifiEnabler(Context context, SwitchWidgetController switchWidgetController, MetricsFeatureProvider metricsFeatureProvider) {
        this(context, switchWidgetController, metricsFeatureProvider, (ConnectivityManager) context.getSystemService("connectivity"));
    }

    WifiEnabler(Context context, SwitchWidgetController switchWidgetController, MetricsFeatureProvider metricsFeatureProvider, ConnectivityManager connectivityManager) {
        this.mListeningToOnSwitchChange = false;
        this.mConnected = new AtomicBoolean(false);
        this.mReceiver = new BroadcastReceiver() { // from class: com.android.settings.wifi.WifiEnabler.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if ("android.net.wifi.WIFI_STATE_CHANGED".equals(action)) {
                    WifiEnabler.this.handleWifiStateChanged(WifiEnabler.this.mWifiManager.getWifiState());
                } else if ("android.net.wifi.supplicant.STATE_CHANGE".equals(action)) {
                    if (!WifiEnabler.this.mConnected.get()) {
                        WifiEnabler.this.handleStateChanged(android.net.wifi.WifiInfo.getDetailedStateOf((SupplicantState) intent.getParcelableExtra("newState")));
                    }
                } else if ("android.net.wifi.STATE_CHANGE".equals(action)) {
                    NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    WifiEnabler.this.mConnected.set(networkInfo.isConnected());
                    WifiEnabler.this.handleStateChanged(networkInfo.getDetailedState());
                }
            }
        };
        this.mContext = context;
        this.mSwitchWidget = switchWidgetController;
        this.mSwitchWidget.setListener(this);
        this.mMetricsFeatureProvider = metricsFeatureProvider;
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mConnectivityManager = connectivityManager;
        this.mIntentFilter = new IntentFilter("android.net.wifi.WIFI_STATE_CHANGED");
        this.mIntentFilter.addAction("android.net.wifi.supplicant.STATE_CHANGE");
        this.mIntentFilter.addAction("android.net.wifi.STATE_CHANGE");
        this.mWifiSettingsExt = UtilsExt.getWifiSettingsExt(this.mContext);
        setupSwitchController();
    }

    public void setupSwitchController() {
        handleWifiStateChanged(this.mWifiManager.getWifiState());
        if (!this.mListeningToOnSwitchChange) {
            this.mSwitchWidget.startListening();
            this.mListeningToOnSwitchChange = true;
        }
        this.mSwitchWidget.setupView();
    }

    public void teardownSwitchController() {
        if (this.mListeningToOnSwitchChange) {
            this.mSwitchWidget.stopListening();
            this.mListeningToOnSwitchChange = false;
        }
        this.mSwitchWidget.teardownView();
    }

    public void resume(Context context) {
        this.mContext = context;
        this.mContext.registerReceiver(this.mReceiver, this.mIntentFilter);
        if (!this.mListeningToOnSwitchChange) {
            this.mSwitchWidget.startListening();
            this.mListeningToOnSwitchChange = true;
        }
    }

    public void pause() {
        this.mContext.unregisterReceiver(this.mReceiver);
        if (this.mListeningToOnSwitchChange) {
            this.mSwitchWidget.stopListening();
            this.mListeningToOnSwitchChange = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleWifiStateChanged(int i) {
        Log.d("WifiEnabler", "handleWifiStateChanged, state = " + i);
        this.mSwitchWidget.setDisabledByAdmin(null);
        switch (i) {
            case 0:
            case 2:
                break;
            case 1:
                setSwitchBarChecked(false);
                this.mSwitchWidget.setEnabled(true);
                break;
            case 3:
                setSwitchBarChecked(true);
                this.mSwitchWidget.setEnabled(true);
                break;
            default:
                setSwitchBarChecked(false);
                this.mSwitchWidget.setEnabled(true);
                break;
        }
        if (RestrictedLockUtils.hasBaseUserRestriction(this.mContext, "no_config_tethering", UserHandle.myUserId())) {
            this.mSwitchWidget.setEnabled(false);
            return;
        }
        this.mSwitchWidget.setDisabledByAdmin(RestrictedLockUtils.checkIfRestrictionEnforced(this.mContext, "no_config_tethering", UserHandle.myUserId()));
    }

    private void setSwitchBarChecked(boolean z) {
        this.mStateMachineEvent = true;
        this.mSwitchWidget.setChecked(z);
        this.mWifiSettingsExt.customRefreshButtonStatus(z);
        this.mStateMachineEvent = false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleStateChanged(NetworkInfo.DetailedState detailedState) {
    }

    @Override // com.android.settings.widget.SwitchWidgetController.OnSwitchChangeListener
    public boolean onSwitchToggled(boolean z) {
        if (this.mStateMachineEvent) {
            return true;
        }
        if (z && !WirelessUtils.isRadioAllowed(this.mContext, "wifi")) {
            Toast.makeText(this.mContext, (int) R.string.wifi_in_airplane_mode, 0).show();
            this.mSwitchWidget.setChecked(false);
            this.mWifiSettingsExt.customRefreshButtonStatus(false);
            return false;
        }
        if (z) {
            this.mMetricsFeatureProvider.action(this.mContext, 139, new Pair[0]);
        } else {
            this.mMetricsFeatureProvider.action(this.mContext, 138, this.mConnected.get());
        }
        Log.d("WifiEnabler", "onSwitchToggled11, isChecked = " + z);
        if (!this.mWifiManager.setWifiEnabled(z)) {
            this.mSwitchWidget.setEnabled(true);
            Toast.makeText(this.mContext, (int) R.string.wifi_error, 0).show();
        }
        return true;
    }
}
