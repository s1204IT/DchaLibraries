package com.android.settingslib.deviceinfo;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import com.android.settingslib.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

/* loaded from: classes.dex */
public abstract class AbstractBluetoothAddressPreferenceController extends AbstractConnectivityPreferenceController {
    private static final String[] CONNECTIVITY_INTENTS = {"android.bluetooth.adapter.action.STATE_CHANGED"};
    static final String KEY_BT_ADDRESS = "bt_address";
    private Preference mBtAddress;

    public AbstractBluetoothAddressPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, lifecycle);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return BluetoothAdapter.getDefaultAdapter() != null;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return KEY_BT_ADDRESS;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mBtAddress = preferenceScreen.findPreference(KEY_BT_ADDRESS);
        updateConnectivity();
    }

    @Override // com.android.settingslib.deviceinfo.AbstractConnectivityPreferenceController
    protected String[] getConnectivityIntents() {
        return CONNECTIVITY_INTENTS;
    }

    @Override // com.android.settingslib.deviceinfo.AbstractConnectivityPreferenceController
    @SuppressLint({"HardwareIds"})
    protected void updateConnectivity() {
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        if (defaultAdapter != null && this.mBtAddress != null) {
            String address = defaultAdapter.isEnabled() ? defaultAdapter.getAddress() : null;
            if (!TextUtils.isEmpty(address)) {
                this.mBtAddress.setSummary(address.toLowerCase());
            } else {
                this.mBtAddress.setSummary(R.string.status_unavailable);
            }
        }
    }
}
