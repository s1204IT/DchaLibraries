package com.android.settings.wifi.tether;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
public abstract class WifiTetherBasePreferenceController extends AbstractPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    protected final ConnectivityManager mCm;
    protected final OnTetherConfigUpdateListener mListener;
    protected Preference mPreference;
    protected final WifiManager mWifiManager;
    protected final String[] mWifiRegexs;

    public interface OnTetherConfigUpdateListener {
        void onNetworkReset();

        void onSecurityChanged();

        void onTetherConfigUpdated();
    }

    public abstract void updateDisplay();

    public WifiTetherBasePreferenceController(Context context, OnTetherConfigUpdateListener onTetherConfigUpdateListener) {
        super(context);
        this.mListener = onTetherConfigUpdateListener;
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mCm = (ConnectivityManager) context.getSystemService("connectivity");
        this.mWifiRegexs = this.mCm.getTetherableWifiRegexs();
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return (this.mWifiManager == null || this.mWifiRegexs == null || this.mWifiRegexs.length <= 0) ? false : true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = preferenceScreen.findPreference(getPreferenceKey());
        updateDisplay();
    }
}
