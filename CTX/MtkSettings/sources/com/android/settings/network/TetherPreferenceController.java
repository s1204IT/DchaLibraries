package com.android.settings.network;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.TetherSettings;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.Utils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import java.util.concurrent.atomic.AtomicReference;

public class TetherPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin, LifecycleObserver, OnCreate, OnDestroy, OnPause, OnResume {
    private final boolean mAdminDisallowedTetherConfig;
    private SettingObserver mAirplaneModeObserver;
    private final BluetoothAdapter mBluetoothAdapter;
    private final AtomicReference<BluetoothPan> mBluetoothPan;
    final BluetoothProfile.ServiceListener mBtProfileServiceListener;
    private final ConnectivityManager mConnectivityManager;
    private Preference mPreference;
    private TetherBroadcastReceiver mTetherReceiver;

    TetherPreferenceController() {
        super(null);
        this.mBtProfileServiceListener = new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
                TetherPreferenceController.this.mBluetoothPan.set((BluetoothPan) bluetoothProfile);
                TetherPreferenceController.this.updateSummary();
            }

            @Override
            public void onServiceDisconnected(int i) {
                TetherPreferenceController.this.mBluetoothPan.set(null);
            }
        };
        this.mAdminDisallowedTetherConfig = false;
        this.mBluetoothPan = new AtomicReference<>();
        this.mConnectivityManager = null;
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public TetherPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        this.mBtProfileServiceListener = new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
                TetherPreferenceController.this.mBluetoothPan.set((BluetoothPan) bluetoothProfile);
                TetherPreferenceController.this.updateSummary();
            }

            @Override
            public void onServiceDisconnected(int i) {
                TetherPreferenceController.this.mBluetoothPan.set(null);
            }
        };
        this.mBluetoothPan = new AtomicReference<>();
        this.mAdminDisallowedTetherConfig = isTetherConfigDisallowed(context);
        this.mConnectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = preferenceScreen.findPreference("tether_settings");
        if (this.mPreference != null && !this.mAdminDisallowedTetherConfig) {
            this.mPreference.setTitle(Utils.getTetheringLabel(this.mConnectivityManager));
            this.mPreference.setEnabled(!TetherSettings.isProvisioningNeededButUnavailable(this.mContext));
        }
    }

    @Override
    public boolean isAvailable() {
        boolean z;
        if ((this.mConnectivityManager.isTetheringSupported() || this.mAdminDisallowedTetherConfig) && !RestrictedLockUtils.hasBaseUserRestriction(this.mContext, "no_config_tethering", UserHandle.myUserId())) {
            z = false;
        } else {
            z = true;
        }
        return !z;
    }

    @Override
    public void updateState(Preference preference) {
        updateSummary();
    }

    @Override
    public String getPreferenceKey() {
        return "tether_settings";
    }

    @Override
    public void onCreate(Bundle bundle) {
        if (this.mBluetoothAdapter != null && this.mBluetoothAdapter.getState() == 12) {
            this.mBluetoothAdapter.getProfileProxy(this.mContext, this.mBtProfileServiceListener, 5);
        }
    }

    @Override
    public void onResume() {
        if (this.mAirplaneModeObserver == null) {
            this.mAirplaneModeObserver = new SettingObserver();
        }
        if (this.mTetherReceiver == null) {
            this.mTetherReceiver = new TetherBroadcastReceiver();
        }
        this.mContext.registerReceiver(this.mTetherReceiver, new IntentFilter("android.net.conn.TETHER_STATE_CHANGED"));
        this.mContext.getContentResolver().registerContentObserver(this.mAirplaneModeObserver.uri, false, this.mAirplaneModeObserver);
    }

    @Override
    public void onPause() {
        if (this.mAirplaneModeObserver != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mAirplaneModeObserver);
        }
        if (this.mTetherReceiver != null) {
            this.mContext.unregisterReceiver(this.mTetherReceiver);
        }
    }

    @Override
    public void onDestroy() {
        BluetoothProfile andSet = this.mBluetoothPan.getAndSet(null);
        if (andSet != null && this.mBluetoothAdapter != null) {
            this.mBluetoothAdapter.closeProfileProxy(5, andSet);
        }
    }

    public static boolean isTetherConfigDisallowed(Context context) {
        return RestrictedLockUtils.checkIfRestrictionEnforced(context, "no_config_tethering", UserHandle.myUserId()) != null;
    }

    void updateSummary() {
        boolean z;
        boolean z2;
        if (this.mPreference == null) {
            return;
        }
        String[] tetheredIfaces = this.mConnectivityManager.getTetheredIfaces();
        String[] tetherableWifiRegexs = this.mConnectivityManager.getTetherableWifiRegexs();
        String[] tetherableBluetoothRegexs = this.mConnectivityManager.getTetherableBluetoothRegexs();
        if (tetheredIfaces == null) {
            z = false;
            z2 = false;
        } else {
            if (tetherableWifiRegexs != null) {
                z2 = false;
                for (String str : tetheredIfaces) {
                    int length = tetherableWifiRegexs.length;
                    int i = 0;
                    while (true) {
                        if (i >= length) {
                            break;
                        }
                        if (str.matches(tetherableWifiRegexs[i])) {
                            z2 = true;
                            break;
                        }
                        i++;
                    }
                }
            } else {
                z2 = false;
            }
            z = tetheredIfaces.length > 1 ? true : tetheredIfaces.length == 1 ? !z2 : false;
        }
        if (!z && tetherableBluetoothRegexs != null && tetherableBluetoothRegexs.length > 0 && this.mBluetoothAdapter != null && this.mBluetoothAdapter.getState() == 12) {
            BluetoothPan bluetoothPan = this.mBluetoothPan.get();
            z = bluetoothPan != null && bluetoothPan.isTetheringOn();
        }
        if (!z2 && !z) {
            this.mPreference.setSummary(R.string.switch_off_text);
            return;
        }
        if (z2 && z) {
            this.mPreference.setSummary(R.string.tether_settings_summary_hotspot_on_tether_on);
        } else if (z2) {
            this.mPreference.setSummary(R.string.tether_settings_summary_hotspot_on_tether_off);
        } else {
            this.mPreference.setSummary(R.string.tether_settings_summary_hotspot_off_tether_on);
        }
    }

    private void updateSummaryToOff() {
        if (this.mPreference == null) {
            return;
        }
        this.mPreference.setSummary(R.string.switch_off_text);
    }

    class SettingObserver extends ContentObserver {
        public final Uri uri;

        public SettingObserver() {
            super(new Handler());
            this.uri = Settings.Global.getUriFor("airplane_mode_on");
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            super.onChange(z, uri);
            if (this.uri.equals(uri)) {
                if (Settings.Global.getInt(TetherPreferenceController.this.mContext.getContentResolver(), "airplane_mode_on", 0) != 0) {
                    TetherPreferenceController.this.updateSummaryToOff();
                }
            }
        }
    }

    class TetherBroadcastReceiver extends BroadcastReceiver {
        TetherBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            TetherPreferenceController.this.updateSummary();
        }
    }
}
